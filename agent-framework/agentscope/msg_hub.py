import asyncio
from typing import List, Dict, Optional, Any


class MsgHub:
    """消息中心 - 负责智能体间的通信路由和分发"""
    
    def __init__(self, agents: List[Any], enable_auto_broadcast: bool = True, announcement: Optional[str] = None):
        """
        初始化消息中心
        
        Args:
            agents: 参与通信的智能体列表
            enable_auto_broadcast: 是否自动广播消息
            announcement: 初始公告消息
        """
        self.agents = agents
        self.enable_auto_broadcast = enable_auto_broadcast
        self.announcement = announcement
        self.message_history = []
    
    async def __aenter__(self):
        """进入上下文管理器"""
        if self.announcement:
            await self.broadcast(self.announcement)
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """退出上下文管理器"""
        pass
    
    async def broadcast(self, message: str):
        """
        广播消息给所有智能体
        
        Args:
            message: 要广播的消息
        """
        self.message_history.append({"type": "broadcast", "content": message})
        
        # 并行发送消息给所有智能体
        tasks = []
        for agent in self.agents:
            if hasattr(agent, "receive_message"):
                tasks.append(agent.receive_message(message))
        
        if tasks:
            await asyncio.gather(*tasks)
    
    async def send(self, message: str, recipient: Any):
        """
        发送消息给特定智能体
        
        Args:
            message: 要发送的消息
            recipient: 接收消息的智能体
        """
        self.message_history.append({"type": "direct", "content": message, "recipient": recipient.name})
        
        if hasattr(recipient, "receive_message"):
            await recipient.receive_message(message)
    
    def set_auto_broadcast(self, enable: bool):
        """
        设置自动广播模式
        
        Args:
            enable: 是否启用自动广播
        """
        self.enable_auto_broadcast = enable
    
    def get_message_history(self) -> List[Dict[str, Any]]:
        """
        获取消息历史
        
        Returns:
            消息历史列表
        """
        return self.message_history
