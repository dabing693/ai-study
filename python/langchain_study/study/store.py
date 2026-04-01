from dataclasses import dataclass

from langchain.agents import create_agent
from langchain.tools import tool, ToolRuntime
from langchain_core.messages import AIMessage
from langgraph.store.memory import InMemoryStore
from pydantic import BaseModel, Field

from model_factory import model


@dataclass
class Context:
    user_id: str


# InMemoryStore 将数据保存到内存字典中。在生产环境中使用基于数据库的存储。
store = InMemoryStore()

# 使用 put 方法向 store 写入示例数据
store.put(
    ("users",),  # 用于将相关数据分组的命名空间（用于用户数据的 users 命名空间）
    "user_123",  # 命名空间内的 Key（用户 ID 作为 Key）
    {
        "name": "小辉",
        "language": "中文",
    }  # 为给定用户存储的数据
)


@tool
def get_user_info(runtime: ToolRuntime[Context]) -> str:
    """Look up user info."""
    # 访问 store - 与提供给 `create_agent` 的 store 相同
    store = runtime.store  # [!code highlight]
    user_id = runtime.context.user_id
    # 从 store 检索数据 - 返回带有 value 和 metadata 的 StoreValue 对象
    user_info = store.get(("users",), user_id)  # [!code highlight]
    return str(user_info.value) if user_info else "Unknown user"


# 允许智能体更新用户信息的工具（适用于聊天应用）
class UserInfo(BaseModel):
    name: str
    age: int
    hobby: str
    language: str
    extra: dict = Field(description='存放额外的与用户相关的偏好和事实性的信息')


@tool
def save_user_info(user_info: UserInfo, runtime: ToolRuntime[Context]) -> str:
    """Save user info."""
    # 访问 store - 与提供给 `create_agent` 的 store 相同
    store = runtime.store  # [!code highlight]
    user_id = runtime.context.user_id  # [!code highlight]
    # 在 store 中存储数据 (namespace, key, data)
    store.put(("users",), user_id, user_info)  # [!code highlight]
    return "Successfully saved user info."


agent = create_agent(
    model=model,
    tools=[get_user_info, save_user_info],
    # 将 store 传递给智能体 - 使智能体能够在运行工具时访问 store
    store=store,
    context_schema=Context
)
print("请问有啥可以帮助你~（输入quit退出）")
while True:
    query = input()
    if query == 'quit':
        break
    # 运行智能体
    res = agent.invoke(
        {"messages": [{"role": "user", "content": query}]},
        context=Context(user_id="user_123")
    )
    reply: AIMessage = res['messages'][-1]
    print(f"AI：{reply.content}")
