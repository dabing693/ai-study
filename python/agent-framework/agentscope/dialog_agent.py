from typing import Optional, Dict, Any, Type, List
from pydantic import BaseModel
import json


class DialogAgent:
    """对话智能体 - 用于角色扮演和交互"""

    def __init__(self, name: str, system_prompt: str):
        """
        初始化对话智能体
        
        Args:
            name: 智能体名称
            system_prompt: 系统提示词
        """
        self.name = name
        self.system_prompt = system_prompt
        self.message_history = []

    async def __call__(self, message: Optional[str] = None, structured_model: Optional[Type[BaseModel]] = None) -> Any:
        """
        调用智能体进行对话
        
        Args:
            message: 输入消息
            structured_model: 结构化输出模型
        
        Returns:
            智能体的响应
        """
        # 构建消息列表
        messages = [
            {"role": "system", "content": self.system_prompt}
        ]
        field_desc = ''
        field_dict = {}
        ind = 1
        for key, field in structured_model.model_fields.items():
            field_desc += f'- {key}: {field.description}（类型: {str(field.annotation)}）\n'
            field_dict[key] = 'value' + str(ind)
            ind += 1
        output_json_example = f"""
        输出JSON格式实例：
        字段解释：
        {field_desc}
        {json.dumps(field_dict)}
        """
        # 添加当前消息
        if message:
            messages.append({"role": "user", "content": message + "\n\n" + output_json_example})
            self.message_history.append({"role": "user", "content": message})

        # 调用智谱大模型
        try:
            response_content = await self._call_chatglm(messages)
            self.message_history.append({"role": "assistant", "content": response_content})

            if structured_model:
                # 尝试解析为结构化输出
                try:
                    # 提取JSON部分
                    if "```json" in response_content:
                        json_str = response_content.split("```json")[1].split("```")[0]
                    else:
                        json_str = response_content

                    data = json.loads(json_str)

                    # 检查数据结构是否符合要求
                    required_fields = set(structured_model.model_fields.keys())
                    if not required_fields.issubset(data.keys()):
                        # 如果缺少必填字段，返回默认响应
                        print(f"⚠️ 结构化输出缺少必填字段: {required_fields - data.keys()}，数据：{data}")
                        return self._get_default_response(structured_model)

                    return structured_model(**data)
                except Exception as e:
                    print(f"⚠️ 解析结构化输出时出错: {e}")
                    # 如果解析失败，返回默认响应
                    return self._get_default_response(structured_model)
            return response_content
        except Exception as e:
            print(f"⚠️ 调用模型时出错: {e}")
            # 出错时返回模拟响应
            response_content = f"{self.name}的响应: {message}"
            self.message_history.append({"role": "assistant", "content": response_content})

            if structured_model:
                return self._get_default_response(structured_model)
            return response_content

    async def _call_chatglm(self, messages: List[Dict[str, str]]) -> str:
        """
        调用智谱ChatGLM大模型
        
        Args:
            messages: 消息列表
        
        Returns:
            模型响应
        """
        from zhipuai import ZhipuAI

        client = ZhipuAI(api_key='62d5b9049126430f9255d00f7a72c91e.qa240op6bmKv3Axq')

        response = client.chat.completions.create(
            model="glm-4-flash",
            messages=messages,
            stream=False,
        )
        return response.choices[0].message.content

    def _get_default_response(self, structured_model: Type[BaseModel]) -> Any:
        """
        获取默认响应
        
        Args:
            structured_model: 结构化输出模型
        
        Returns:
            默认响应
        """
        if structured_model.__name__ == "DiscussionModelCN":
            return structured_model(
                reach_agreement=False,
                confidence_level=5,
                key_evidence="暂时无法分析"
            )
        elif structured_model.__name__ == "WerewolfKillModelCN":
            return structured_model(target_name="曹操")
        elif structured_model.__name__ == "WitchActionModelCN":
            return structured_model(
                use_antidote=False,
                use_poison=False,
                target_name=None
            )
        elif structured_model.__name__ == "SeerActionModelCN":
            return structured_model(target_name="孙权")
        elif structured_model.__name__ == "VoteModelCN":
            return structured_model(target_name="孙权")
        else:
            return structured_model()

    async def receive_message(self, message: str):
        """
        接收消息
        
        Args:
            message: 收到的消息
        """
        self.message_history.append({"role": "system", "content": message})

    def get_message_history(self) -> List[Dict[str, Any]]:
        """
        获取消息历史
        
        Returns:
            消息历史列表
        """
        return self.message_history
