from autogen_core.models import UserMessage, CreateResult
from autogen_ext.models.openai import OpenAIChatCompletionClient
import os
import asyncio


def create_openai_model_client() -> OpenAIChatCompletionClient:
    """创建并配置 OpenAI 模型客户端"""
    return OpenAIChatCompletionClient(
        model=os.getenv("LLM_MODEL_ID", "glm-4-flash"),
        api_key='62d5b9049126430f9255d00f7a72c91e.qa240op6bmKv3Axq',
        base_url=os.getenv("LLM_BASE_URL", "https://open.bigmodel.cn/api/paas/v4"),
        model_info={
            "max_tokens": 4096,
            "input_price_per_token": 0,
            "output_price_per_token": 0,
            "supports_completion": True,
            "supports_chat": True,
            "vision": False,
            "function_calling": False,
            "json_output": False,
            "family": "GLM"
        }
    )


async def main():
    client = create_openai_model_client()
    messages = {"source": "user", "content": "你好，请介绍一下你自己。"}
    msg = UserMessage(**messages)

    response: CreateResult = await client.create(messages=[msg])
    reply = response.content
    print("模型回复：", reply)


if __name__ == '__main__':
    asyncio.run(main())
