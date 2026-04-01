from abc import ABC
from typing import Any

from langchain_core.tools import BaseTool
from langchain_core.tools import StructuredTool
from langchain.agents import create_agent
from langchain_core.messages import AIMessage
from langchain_core.tools import tool
from langgraph.checkpoint.memory import InMemorySaver
from model_factory import model


@tool(description='根据用户id查询用户信息')
def get_weather(city: str):
    return {'city': city, "天气": "晴天", "温度": '26度'}


class SearchTool(BaseTool, ABC):
    name: str = "search_tool"
    description: str = "联网搜索信息"

    def _run(self, *args: Any, **kwargs: Any) -> Any:
        q = kwargs.get('input')
        return [
            {'title': '贵州茅台大涨5%', 'text': '今天盘中贵州茅台一度大涨5%！市值达到3w亿。'},
            {'title': '贵州茅台董事长变动', 'text': '贵州茅台董事长换了。'},
        ]


def add(num1: int, num2: int):
    return num1 + num2


add2Num = StructuredTool.from_function(func=add, name='add2Num', description='计算两个数的和')

agent = create_agent(
    model,
    [get_weather, SearchTool(), add2Num],
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
