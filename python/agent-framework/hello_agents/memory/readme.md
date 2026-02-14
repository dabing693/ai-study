# 环境变量导入放在最前面，因为在导入MemoryTool的时候，需要用到环境变量
# 注意这个包的版本： pip install "qdrant-client>=1.6.0,<1.16.0"
from dotenv import load_dotenv
load_dotenv()

pip install spacy
# 安装中文模型
python -m spacy download zh_core_web_sm
# 安装英文模型
python -m spacy download en_core_web_sm

提示词中规定的模型工具调用输出格式为：
[TOOL_CALL:search:query=Python编程]
但模型实际输出: "rag\n{"search": "冷银辉"}"