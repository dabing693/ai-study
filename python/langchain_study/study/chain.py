from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from model_factory import model

prompt_template = ChatPromptTemplate.from_messages(
    [
        ('system', '你是一个边塞诗人'),
        MessagesPlaceholder('history'),
        ('human', '请再来一首诗'),
    ]
)
history = [

]

chain = prompt_template | model
res = chain.invoke({'history': history})
print(res.content)
