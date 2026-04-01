import time

from langchain_core.caches import InMemoryCache
from langchain_core.outputs import Generation
from model_factory import model, model_with_cache

# 初始化缓存
cache = InMemoryCache()


def set_get():
    # 更新缓存
    cache.update(
        prompt="What is the capital of France?",
        llm_string="model='gpt-3.5-turbo', temperature=0.1",
        return_val=[Generation(text="Paris")],
    )

    # 查找缓存
    result = cache.lookup(
        prompt="What is the capital of France?",
        llm_string="model='gpt-3.5-turbo', temperature=0.1",
    )
    # result 是 [Generation(text="Paris")]
    print(result)


def local():
    start = time.time_ns()
    # 第一次调用会访问 API
    result1 = model_with_cache.invoke("你好")  # "hello"
    print(result1.content)
    print(f"耗时：{(time.time_ns() - start) / 10e6}")
    start = time.time_ns()
    # 第二次相同调用会从缓存返回
    result2 = model_with_cache.invoke("你好")  # "hello" (来自缓存)
    print(result2.content)
    print(f"耗时：{(time.time_ns() - start) / 10e6}")


def global_test():
    from langchain_core.globals import set_llm_cache
    # 设置全局缓存
    global_cache = InMemoryCache()
    set_llm_cache(global_cache)

    try:
        start = time.time_ns()
        # 第一次调用会访问 API
        result1 = model.invoke("你好")  # "hello"
        print(result1.content)
        print(f"耗时：{(time.time_ns() - start) / 10e6}")
        start = time.time_ns()
        # 第二次相同调用会从缓存返回
        result2 = model.invoke("你好")  # "hello" (来自缓存)
        print(result2.content)
        print(f"耗时：{(time.time_ns() - start) / 10e6}")
    finally:
        set_llm_cache(None)  # 清理全局缓存


if __name__ == '__main__':
    global_test()
