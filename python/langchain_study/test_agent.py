import asyncio
import json
import os
import warnings
from typing import Iterator, Any, AsyncIterator, Dict

from dotenv import load_dotenv
from fastapi import FastAPI, Request
from fastapi.responses import StreamingResponse
from jwt import InsecureKeyLengthWarning
from langchain.agents import create_agent
from langchain_community.chat_models.zhipuai import ChatZhipuAI
from langchain_core.messages import AIMessageChunk
from langchain_tavily import TavilySearch
from langgraph.types import StreamMode
from langgraph.checkpoint.memory import InMemorySaver

from tools import *

load_dotenv(override=True)
warnings.filterwarnings("ignore", category=InsecureKeyLengthWarning)
app = FastAPI()

tavily_api_key = os.getenv('TAVILY_API_KEY')
# Use the updated TavilySearch class
web_search = TavilySearch(max_results=2, api_key=tavily_api_key)

tools = [web_search, get_weather, simple_write_file]
memory = InMemorySaver()


def build_agent(model_stream: bool = False):
    model = ChatZhipuAI(
        model='glm-4.5-flash',
        zhipuai_api_key=os.getenv('ZHIPUAI_API_KEY'),
        temperature=0.7,  # Add temperature parameter
        max_tokens=4000,  # Add max_tokens parameter
        streaming=model_stream,
        # 添加超时配置
        timeout=60.0,  # 总超时时间（秒）
        max_retries=3,  # 最大重试次数
    )

    agent = create_agent(model=model, tools=tools, checkpointer=memory,
                         system_prompt='你是一名乐于助人的智能助手')
    return agent


def invoke(prompt: str):
    try:
        config = {
            "configurable": {
                "thread_id": "6"
            }
        }
        agent = build_agent()
        result = agent.invoke(
            input={'messages': [{'role': 'user', 'content': prompt}]},
            config=config
        )
        last_msg = result['messages'][-1].content
        print(agent.get_state(config))
        return last_msg
    except Exception as e:
        return f"Error: {e}"


def stream_invoke(prompt: str, stream_mode: StreamMode = "messages") -> Iterator[dict[str, Any] | Any]:
    try:
        config = {
            "configurable": {
                "thread_id": "6"
            }
        }
        result = build_agent(model_stream=True).stream(
            input={'messages': [{'role': 'user', 'content': prompt}]},
            config=config,
            stream_mode=stream_mode
        )
        return result
    except Exception as e:
        return f"Error: {e}"


def astream_invoke(prompt: str, stream_mode: StreamMode = "messages") -> AsyncIterator[dict[str, Any] | Any] | str:
    try:
        config = {
            "configurable": {
                "thread_id": "6"
            }
        }
        result = build_agent(model_stream=True).astream(
            input={'messages': [{'role': 'user', 'content': prompt}]},
            config=config,
            stream_mode=stream_mode
        )
        return result
    except Exception as e:
        return f"Error: {e}"


def sse_chunk(content: str):
    r = f"data: {content}\n\n"
    print(r)
    return r


async def generate(query: str, stream_mode: StreamMode, asynchronous: bool):
    if asynchronous:
        async for it in astream_invoke(query, stream_mode=stream_mode):
            if isinstance(it, tuple):
                msg = it[0]
                if isinstance(msg, AIMessageChunk):
                    text = msg.content.strip() if isinstance(msg.content, str) else None
                    if text:
                        r = json.dumps(msg.model_dump(), ensure_ascii=False)
                        yield sse_chunk(r)
                else:
                    r = json.dumps(msg.model_dump(), ensure_ascii=False)
                    yield sse_chunk(r)
    else:
        for it in stream_invoke(query, stream_mode=stream_mode):
            if isinstance(it, tuple):
                msg = it[0]
                if isinstance(msg, AIMessageChunk):
                    text = msg.content.strip() if isinstance(msg.content, str) else None
                    if text:
                        r = json.dumps(msg.model_dump(), ensure_ascii=False)
                        yield sse_chunk(r)
                        await asyncio.sleep(0)
                else:
                    r = json.dumps(msg.model_dump(), ensure_ascii=False)
                    yield sse_chunk(r)
                await asyncio.sleep(0)


@app.post("/v1/responses")
async def responses(req: dict, request: Request):
    query = req.get('question')
    stream_mode = req.get('stream_mode')
    asynchronous = req.get('async', True)
    if not stream_mode or stream_mode == 'no':
        return invoke(query)

    return StreamingResponse(
        generate(query, stream_mode, asynchronous),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Content-Type-Options": "nosniff"
        }
    )


if __name__ == '__main__':
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
