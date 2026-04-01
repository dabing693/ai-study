from langchain_community.vectorstores import Milvus
from langchain_core.documents import Document
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough, RunnableLambda
from langchain_text_splitters import RecursiveCharacterTextSplitter

from model_factory import *

template = """根据下面的内容回答问题:  
{context}  

问题: {question}  
"""
prompt = ChatPromptTemplate.from_template(template)


def save_documents():
    splitter = RecursiveCharacterTextSplitter(
        separators=['# ', '## ', '### ', '#### '],
        chunk_size=500,
        chunk_overlap=50,
        length_function=len,
    )
    md_file = r'D:\Study\git\ai-study\python\langchain_study\study\data\第九章 上下文工程.md'
    with open(md_file, encoding='utf-8') as f:
        lines = [i.strip() for i in f.readlines() if i.strip() != '']
        file_content = ''.join(lines)
    texts = splitter.split_text(file_content)[:20]
    documents = [Document(page_content=i) for i in texts]
    # 创建 Milvus 向量存储
    vectorstore = Milvus.from_documents(
        documents=documents,
        embedding=embeddings_model,
        connection_args={"host": "localhost", "port": "19530"}
    )


def rag():
    # 创建 Milvus 向量存储
    vectorstore = Milvus.from_documents(
        documents=[],
        embedding=embeddings_model,
        connection_args={"host": "localhost", "port": "19530"}
    )

    def print_prompt(prompt_template: ChatPromptTemplate):
        for msg in prompt_template.messages:
            print(msg.content)
            print('*' * 40)
        return prompt_template

    def rewrite_context(inputs: dict) -> dict:
        key = 'context'
        if key in inputs:
            context = inputs.get(key)
            pages = [f"文档{i + 1}：\n{context[i].page_content}" for i in range(len(context))]
            inputs[key] = '\n\n'.join(pages)
        return inputs

    # 创建检索器
    retriever = vectorstore.as_retriever()

    rag_chain = (
            {"context": retriever, "question": RunnablePassthrough()}
            | RunnableLambda(rewrite_context)
            | prompt
            | RunnableLambda(print_prompt)
            | model
            | StrOutputParser()
    )
    res = rag_chain.invoke('什么是上下文工程')
    print(res)


if __name__ == '__main__':
    rag()
