from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough

from model_factory import *

prompt = ChatPromptTemplate.from_messages(
    [
        ('system', '你是一个乐于助人的助理'),
        ('human', '{question}'),
    ]
)

chain = prompt | {
    "iflow": model,
    "zhipu": zhipu_model,
    "passthrough": RunnablePassthrough(),
}

iflow = ''
zhipu = ''
for chunk in chain.stream({"question": "你好"}):
    # 每次只输出一个键的值
    print(chunk)
    if 'iflow' in chunk:
        iflow += chunk['iflow'].content
    if 'zhipu' in chunk:
        zhipu += chunk['zhipu'].content
print(iflow)
print("-" * 30)
print(zhipu)
