6.3.2 三国狼人杀游戏
为了深入理解 AgentScope 的消息驱动架构和多智能体协作能力，我们将构建一个融合了中国古典文化元素的"三国狼人杀"游戏。这个案例不仅展示了 AgentScope 在处理复杂多智能体交互方面的优势，更重要的是，它演示了如何在一个需要实时协作、角色扮演和策略博弈的场景中，充分发挥消息驱动架构的威力。与传统狼人杀不同，我们的"三国狼人杀"将刘备、关羽、诸葛亮等经典角色引入游戏，每个智能体不仅要完成狼人杀的基本任务（如狼人击杀、预言家查验、村民推理），还要体现出对应三国人物的性格特点和行为模式。这种设计让我们能够观察到 AgentScope 在处理多层次角色建模方面的表现。

（1）架构设计与核心组件

本案例的系统设计遵循了分层解耦的原则，将游戏逻辑划分为三个独立的层次，每个层次都映射了 AgentScope 的一个或多个核心组件：

游戏控制层 (Game Control Layer)：由一个 ThreeKingdomsWerewolfGame 类作为游戏的主控制器，负责维护全局状态（如玩家存活列表、当前游戏阶段）、推进游戏流程（调用夜晚阶段、白天阶段）以及裁定胜负。
智能体交互层 (Agent Interaction Layer)：完全由 MsgHub 驱动。所有智能体间的通信，无论是狼人间的秘密协商，还是白天的公开辩论，都通过消息中心进行路由和分发。
角色建模层 (Role Modeling Layer)：每个玩家都是一个基于 DialogAgent 的实例。我们通过精心设计的系统提示词，为每个智能体注入了“游戏角色”和“三国人格”的双重身份。
（2）消息驱动的游戏流程

本案例最核心的设计是以消息驱动代替状态机来管理游戏流程。在传统实现中，游戏阶段的转换通常由一个中心化的状态机（State Machine）控制。而在 AgentScope 的范式下，游戏流程被自然地建模为一系列定义好的消息交互模式。

例如，狼人阶段的实现，并非一个简单的函数调用，而是通过 MsgHub 动态创建一个临时的、仅包含狼人玩家的私密通信频道：

async def werewolf_phase(self, round_num: int):
    """狼人阶段 - 展示消息驱动的协作模式"""
    if not self.werewolves:
        return None
        
    # 通过消息中心建立狼人专属通信频道
    async with MsgHub(
        self.werewolves,
        enable_auto_broadcast=True,
        announcement=await self.moderator.announce(
            f"狼人们，请讨论今晚的击杀目标。存活玩家：{format_player_list(self.alive_players)}"
        ),
    ) as werewolves_hub:
        # 讨论阶段：狼人通过消息交换策略
        for _ in range(MAX_DISCUSSION_ROUND):
            for wolf in self.werewolves:
                await wolf(structured_model=DiscussionModelCN)
        
        # 投票阶段：收集并统计狼人的击杀决策
        werewolves_hub.set_auto_broadcast(False)
        kill_votes = await fanout_pipeline(
            self.werewolves,
            msg=await self.moderator.announce("请选择击杀目标"),
            structured_model=WerewolfKillModelCN,
            enable_gather=False,
        )
Copy to clipboardErrorCopied
这种设计的优势在于，游戏逻辑被清晰地表达为“在特定上下文中，以何种模式进行消息交换”，而不是一连串僵硬的状态转换。白天讨论（全员广播）、预言家查验（点对点请求）等阶段也都遵循同样的设计范式。

（3）用结构化输出约束游戏规则

狼人杀游戏的一个关键挑战是如何确保智能体的行为符合游戏规则。AgentScope 的结构化输出机制为这个问题提供了解决方案。我们为不同的游戏行为定义了严格的数据模型：

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

class WitchActionModelCN(BaseModel):
    """女巫行动的输出格式"""
    use_antidote: bool = Field(description="是否使用解药")
    use_poison: bool = Field(description="是否使用毒药")
    target_name: Optional[str] = Field(description="毒药目标玩家姓名")
Copy to clipboardErrorCopied
通过这种方式，我们不仅确保了智能体输出的格式一致性，更重要的是实现了游戏规则的自动化约束。例如，女巫智能体无法同时对同一目标使用解药和毒药，预言家每晚只能查验一名玩家，这些约束都通过数据模型的字段定义和验证逻辑自动执行。

（4）角色建模的双重挑战

在这个案例中，最有趣的技术挑战是如何让智能体同时扮演好两个层面的角色：游戏功能角色（狼人、预言家等）和文化人格角色（刘备、曹操等）。我们通过提示词工程来解决这个问题：

def get_role_prompt(role: str, character: str) -> str:
    """获取角色提示词 - 融合游戏规则与人物性格"""
    base_prompt = f"""你是{character}，在这场三国狼人杀游戏中扮演{role}。

重要规则：
1. 你只能通过对话和推理参与游戏
2. 不要尝试调用任何外部工具或函数
3. 严格按照要求的JSON格式回复

角色特点：
"""
    
    if role == "狼人":
        return base_prompt + f"""
- 你是狼人阵营，目标是消灭所有好人
- 夜晚可以与其他狼人协商击杀目标
- 白天要隐藏身份，误导好人
- 以{character}的性格说话和行动
"""
Copy to clipboardErrorCopied
这种设计让我们观察到了一个有趣的现象：不同的三国人物在扮演相同游戏角色时，会表现出截然不同的策略和话语风格。例如，扮演狼人的"曹操"可能会表现得更加狡猾和善于伪装，而扮演狼人的"张飞"则可能显得更加直接和冲动。

（5）并发处理与容错机制

AgentScope 的异步架构在这个多智能体游戏中发挥了重要作用。游戏中经常出现需要同时收集多个智能体决策的场景，比如投票阶段：

# 并行收集所有玩家的投票决策
vote_msgs = await fanout_pipeline(
    self.alive_players,
    await self.moderator.announce("请投票选择要淘汰的玩家"),
    structured_model=get_vote_model_cn(self.alive_players),
    enable_gather=False,
)
Copy to clipboardErrorCopied
fanout_pipeline 允许我们并行地向所有智能体发送相同的消息，并异步收集它们的响应。这不仅提高了游戏的执行效率，更重要的是模拟了真实狼人杀游戏中"同时投票"的场景。同时，我们在关键环节加入了容错处理：

try:
    response = await wolf(
        "请分析当前局势并表达你的观点。",
        structured_model=DiscussionModelCN
    )
except Exception as e:
    print(f"⚠️ {wolf.name} 讨论时出错: {e}")
    # 创建默认响应，确保游戏继续进行
    default_response = DiscussionModelCN(
        reach_agreement=False,
        confidence_level=5,
        key_evidence="暂时无法分析"
    )
Copy to clipboardErrorCopied
这种设计确保了即使某个智能体出现异常，整个游戏流程也能继续进行。

（6）案例输出与总结

为了更直观地感受 AgentScope 的运行机制，以下是一段截取自游戏夜晚阶段的真实运行日志，展示了扮演“孙权”和“周瑜”的两个狼人智能体进行秘密协商并执行击杀的过程。

🎮 欢迎来到三国狼人杀！

=== 游戏初始化 ===
游戏主持人: 📢 【孙权】你在这场三国狼人杀中扮演狼人，你的角色是孙权。夜晚可以击杀一名玩家
游戏主持人: 📢 【周瑜】你在这场三国狼人杀中扮演狼人，你的角色是周瑜。夜晚可以击杀一名玩家
...

游戏主持人: 📢 三国狼人杀游戏开始！参与者：孙权、周瑜、曹操、张飞、司马懿、赵云
✅ 游戏设置完成，共6名玩家

=== 第1轮游戏 ===
🌙 第1夜降临，天黑请闭眼...

【狼人阶段】
游戏主持人: 📢 🐺 狼人请睁眼，选择今晚要击杀的目标...
游戏主持人: 📢 狼人们，请讨论今晚的击杀目标。存活玩家：孙权、周瑜、曹操、张飞、司马懿、赵云

孙权: 今晚我们应该除掉周瑜，此人智谋过人，对我们威胁很大。
周瑜: 孙权，你言之有理。但周瑜虽智，却未必是今晚的最大威胁。曹操势力庞大，若不尽早除去，恐对我们不利。
孙权: 曹操的确是个威胁，但周瑜若活着，他能够识破我们的计谋。不如先解决眼前的隐患。
周瑜: 孙权，你的顾虑不无道理。但曹操若与我们为敌，他可以联合其他势力对我们构成更大的威胁。
孙权: 你说的也有道理，曹操的联合确实麻烦。那我们就先对付曹操吧。
周瑜: 很好，孙权。曹操才是我们今晚首要的目标。

游戏主持人: 📢 请选择击杀目标
孙权: 我同意，曹操必须被除掉。
周瑜: 我同意，曹操是我们今晚要解决的目标。

【预言家阶段】
游戏主持人: 📢 🔮 预言家请睁眼，选择要查验的玩家...
曹操: 我要查验孙权。
游戏主持人: 📢 查验结果：孙权是狼人

【女巫阶段】
游戏主持人: 📢 🧙‍♀️ 女巫请睁眼...
游戏主持人: 📢 今晚曹操被狼人击杀
张飞: 我昨晚使用了解药救了曹操，现在解药已经用掉了。
游戏主持人: 📢 你使用解药救了曹操

游戏主持人: 📢 昨夜平安无事，无人死亡。

【白天讨论阶段】
游戏主持人: 📢 ☀️ 第1天天亮了，请大家睁眼...
游戏主持人: 📢 现在开始自由讨论。存活玩家：孙权、周瑜、曹操、张飞、司马懿、赵云

孙权: 诸位，曹操势力庞大，对我们都是潜在的威胁。今晚我建议我们集中力量对付他。
周瑜: 孙权所言极是，曹操不仅自身强大，还可能与其他玩家结盟，对我们构成更大的威胁。
曹操: 我昨晚查验了孙权，本以为他是好人，但游戏主持人给出的结果却是狼人。这说明有狼人在说谎。
张飞: 我昨晚确实救了曹操，说明他是被狼人袭击的。但曹操查验孙权的结果令人怀疑。
司马懿: 曹操的查验结果和张飞的救人行动似乎存在矛盾，我们需要更多的信息来判断谁是狼人。
赵云: 情况确实复杂，我们需要仔细分析各方的发言。

【投票阶段】
游戏主持人: 📢 请投票选择要淘汰的玩家

孙权: 曹操的威胁依然很大，我坚持认为应该投票给他。
周瑜: 基于昨晚的情况，我认为我们应该先投票给曹操，他的威胁最大。
曹操: 我选择投票给孙权。根据游戏主持人的反馈，孙权确实是狼人。
张飞: 我坚持昨晚救了曹操的事实，但孙权被查出是狼人这一点让我感到困惑。
司马懿: 我们需要更多的信息来判断谁是狼人。

[游戏继续...]