import os
from typing import Any

from dotenv import load_dotenv
from langchain.agents import AgentState
from langchain.agents.middleware import wrap_model_call, before_model, ModelRequest
from langchain_community.chat_models import ChatZhipuAI
from langchain_core.language_models import ModelProfile
from langchain_core.messages import HumanMessage, SystemMessage
from langgraph.runtime import Runtime

load_dotenv(override=True)


def build_model(model_stream: bool = False, enable_thinking=True) -> ChatZhipuAI:
    return ChatZhipuAI(
        model='glm-4.5-flash',
        zhipuai_api_key=os.getenv('ZHIPUAI_API_KEY'),
        temperature=0.7,  # Add temperature parameter
        max_tokens=4000,  # Add max_tokens parameter
        streaming=model_stream,
        # 添加超时配置
        timeout=60.0,  # 总超时时间（秒）
        max_retries=3,  # 最大重试次数
        profile=ModelProfile(reasoning_output=False),
        extra_body={
            "chat_template_kwargs": {
                "enable_thinking": enable_thinking
            }
        },
    )


@wrap_model_call
async def dynamic_model_routing(request: ModelRequest, handler):
    messages = request.messages
    last_user_msg = ''
    for msg in reversed(messages):
        if isinstance(msg, HumanMessage):
            if isinstance(msg.content, str):
                last_user_msg = msg.content
            elif isinstance(msg.content, list):
                last_user_msg = msg.content[0].get('text')
            else:
                raise Exception(f'未知的msg.content类型: {type(msg.content)}')
            break
    hard_keywords = ["证明", "推导", "严谨", "chain of thought", "step-by-step", "reason step by step"]
    # 走强模型条件： 历史消息过长 最近用户输入很长 出现复杂任务关键词
    is_hard = len(messages) > 10 or len(last_user_msg) > 120 or any(
        kw.lower() in last_user_msg for kw in hard_keywords)
    if not is_hard:
        request.model = build_model(model_stream=True, enable_thinking=False)
        print("问题不难，模型走非思考模式，减少响应时间")
        print(request.model)
    # 👇async必须await
    return await handler(request)


@before_model
async def trim_messages(state: AgentState, runtime: Runtime) -> dict[str, Any]:
    his_msg = state['messages']
    if len(his_msg) > 4:
        state['messages'] = his_msg[0:1] + his_msg[-3:]
    print()


if __name__ == '__main__':
    model = build_model(model_stream=True, enable_thinking=True)
    messages = [
        SystemMessage(content="/no_think 你是一个乐于助人的助理。"),
        HumanMessage(content="你好，你会啥"),
    ]
    r = model.invoke(messages)
    print(r)
