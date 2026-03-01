from deepagents import create_deep_agent
from src.agent.model import build_model
from src.tools import *
from src.prompts import DEEP_AGENTS__PROMPT


def build_agent():
    model = build_model(model_stream=False)
    tools = [tavily_search, get_weather]
    return create_deep_agent(
        model=model,
        tools=tools,
        system_prompt=DEEP_AGENTS__PROMPT,
    )


agent = build_agent()

if __name__ == '__main__':
    res = agent.invoke(input={"messages": [{"role": "user", "content": "你是谁"}]})
