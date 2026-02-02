from dotenv import load_dotenv

print("加载环境变量")
load_dotenv()
# 配置好同级文件夹下.env中的大模型API
from hello_agents import SimpleAgent, HelloAgentsLLM, ToolRegistry
from hello_agents.tools import MemoryTool, RAGTool

# 创建LLM实例
llm = HelloAgentsLLM(provider="zhipu")

# 创建工具注册表
tool_registry = ToolRegistry()

# 添加记忆工具
memory_tool = MemoryTool(user_id="user123")
tool_registry.register_tool(memory_tool)

# 添加RAG工具
rag_tool = RAGTool(knowledge_base_path="knowledge_base")
rag_tool.execute('add_document', file_path='./knowledge_base/冷银辉-后端开发_1226.pdf')
tool_registry.register_tool(rag_tool)

# 创建Agent
agent = SimpleAgent(
    name="智能助手",
    llm=llm,
    system_prompt="你是一个有记忆和知识检索能力的AI助手",
    # 为Agent配置工具
    tool_registry=tool_registry
)

print("欢迎使用智能问答助手（输入q或者quit退出）")
while True:
    user_input = input('> ')
    if user_input in ['q', 'quit']:
        print("欢迎下次再来")
        break
    if user_input.strip() == '':
        print("请输入内容")
        continue
    # 开始对话
    response = agent.run(user_input)
    print(response)
