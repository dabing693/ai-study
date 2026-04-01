import os

from langchain_core.caches import InMemoryCache
from langchain_openai import ChatOpenAI
from langchain_ollama import ChatOllama, OllamaEmbeddings

model = ChatOpenAI(
    openai_api_base="https://apis.iflow.cn/v1",
    model="qwen3-max",
    openai_api_key=os.getenv('IFLOW_API_KEY'),
    temperature=0.7,
    max_tokens=8192,
    streaming=False,
    # 添加超时配置
    request_timeout=1200,  # 总超时时间（秒）
    max_retries=3,  # 最大重试次数
)

zhipu_model = ChatOpenAI(
    openai_api_base="https://open.bigmodel.cn/api/paas/v4",
    model="glm-4.5-flash",
    openai_api_key='62d5b9049126430f9255d00f7a72c91e.qa240op6bmKv3Axq',
    temperature=0.7,
    max_tokens=8192,
    streaming=False,
    # 添加超时配置
    request_timeout=1200,  # 总超时时间（秒）
    max_retries=3,  # 最大重试次数
)

embeddings_model = OllamaEmbeddings(
    model="qwen3-embedding:0.6b",
)
local_cache = InMemoryCache()
model_with_cache = ChatOllama(
    model="qwen2.5:0.5b",
    cache=local_cache
)
