from pydantic import BaseModel, Field
from typing import Optional


class DiscussionModelCN(BaseModel):
    """讨论阶段的输出格式"""
    reach_agreement: bool = Field(
        description="是否已达成一致意见",
        default=False
    )
    confidence_level: int = Field(
        description="对当前推理的信心程度(1-10)",
        ge=1, le=10,
        default=5
    )
    key_evidence: Optional[str] = Field(
        description="支持你观点的关键证据",
        default=None
    )


class WerewolfKillModelCN(BaseModel):
    """狼人击杀的输出格式"""
    target_name: str = Field(
        description="击杀目标玩家姓名",
        default="曹操"
    )


class WitchActionModelCN(BaseModel):
    """女巫行动的输出格式"""
    use_antidote: bool = Field(description="是否使用解药")
    use_poison: bool = Field(description="是否使用毒药")
    target_name: Optional[str] = Field(description="毒药目标玩家姓名")


class SeerActionModelCN(BaseModel):
    """预言家行动的输出格式"""
    target_name: str = Field(
        description="查验目标玩家姓名",
        default="孙权"
    )


class VoteModelCN(BaseModel):
    """投票的输出格式"""
    target_name: str = Field(
        description="投票目标玩家姓名",
        default="孙权"
    )
