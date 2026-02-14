import asyncio
from typing import List, Optional, Type, Any
from pydantic import BaseModel


async def fanout_pipeline(
    agents: List[Any],
    msg: str,
    structured_model: Optional[Type[BaseModel]] = None,
    enable_gather: bool = False
) -> List[Any]:
    """
    并行向多个智能体发送消息并收集响应
    
    Args:
        agents: 智能体列表
        msg: 要发送的消息
        structured_model: 结构化输出模型
        enable_gather: 是否启用结果收集
    
    Returns:
        智能体的响应列表
    """
    tasks = []
    for agent in agents:
        tasks.append(agent(msg, structured_model=structured_model))
    
    responses = await asyncio.gather(*tasks)
    return responses


def format_player_list(players: List[Any]) -> str:
    """
    格式化玩家列表
    
    Args:
        players: 玩家列表
    
    Returns:
        格式化后的玩家列表字符串
    """
    return ", ".join([player.name for player in players])
