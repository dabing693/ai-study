import os
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv(override=True)
api_key = os.getenv("ZHIPUAI_API_KEY")
client = OpenAI(api_key=api_key, base_url='https://open.bigmodel.cn/api/paas/v4')
response = client.chat.completions.create(model='glm-4.5-flash',
                                          messages=[{'role': 'system', 'content': '你是乐于助人的助手'},
                                                    {'role': 'user', 'content': '你是谁'}])
print(response.choices[0].message.content)
