from langchain.agents import create_agent
from langchain_core.messages import AIMessage
from langchain_core.tools import tool
from langgraph.checkpoint.memory import InMemorySaver
from model_factory import model


@tool(description='根据用户id查询用户信息')
def get_weather(city: str):
    return {'city': city, "天气": "晴天", "温度": '26度'}


agent = create_agent(
    model,
    [get_weather],
    checkpointer=InMemorySaver(),
)
print("> 请问有什么可以帮助你~")
while True:
    question = input()
    if question == 'quit':
        break
    res = agent.invoke(
        {"messages": [{"role": "user", "content": question}]},
        {"configurable": {"thread_id": "1"}},
    )
    reply: AIMessage = res['messages'][-1]
    print(f'AI: {reply.content}')
