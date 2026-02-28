import asyncio
import json
import warnings
from typing import Iterator, Any, AsyncIterator

from fastapi import FastAPI, Request
from fastapi.responses import StreamingResponse
from jwt import InsecureKeyLengthWarning
from langchain.agents import create_agent
from langchain_core.messages import AIMessageChunk
from langchain_tavily import TavilySearch
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.types import StreamMode

from src.agent.model import *
from src.tools import *

load_dotenv(override=True)
warnings.filterwarnings("ignore", category=InsecureKeyLengthWarning)
app = FastAPI()

tavily_api_key = os.getenv('TAVILY_API_KEY')
# Use the updated TavilySearch class
web_search = TavilySearch(max_results=2, api_key=tavily_api_key)

tools = [web_search, get_weather, simple_write_file]
middleware = [trim_messages, dynamic_model_routing, build_summarization_middleware()]
memory = InMemorySaver()


def build_agent(model_stream: bool = False, use_memory: bool = False):
    model = build_model(model_stream)
    if use_memory:
        # langgraph dev会报错：因为使用了langgraph.checkpoint.memory.InMemorySaver
        return create_agent(model=model, tools=tools, checkpointer=memory,
                            system_prompt='你是一名乐于助人的智能助手',
                            middleware=middleware)
    else:
        return create_agent(model=model, tools=tools, system_prompt='你是一名乐于助人的智能助手',
                            middleware=middleware)


stream_agent = build_agent(model_stream=True)
stream_memory_agent = build_agent(model_stream=True, use_memory=True)
no_stream_agent = build_agent()


def invoke(prompt: str):
    try:
        config = {
            "configurable": {
                "thread_id": "6"
            }
        }

        result = no_stream_agent.invoke(
            input={'messages': [{'role': 'user', 'content': prompt}]},
            config=config
        )
        last_msg = result['messages'][-1].content
        print(no_stream_agent.get_state(config))
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
        result = stream_memory_agent.stream(
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
        result = stream_memory_agent.astream(
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
