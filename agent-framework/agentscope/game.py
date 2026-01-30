import asyncio
from typing import List, Dict, Optional, Tuple
from dialog_agent import DialogAgent
from msg_hub import MsgHub
from models import DiscussionModelCN, WerewolfKillModelCN, WitchActionModelCN, SeerActionModelCN, VoteModelCN
from utils import fanout_pipeline, format_player_list
from prompts import get_role_prompt, Moderator

MAX_DISCUSSION_ROUND = 3


class ThreeKingdomsWerewolfGame:
    """三国狼人杀游戏主控制器"""
    
    def __init__(self, players: List[str], characters: List[str]):
        """
        初始化游戏
        
        Args:
            players: 玩家名称列表
            characters: 三国人物列表
        """
        self.players = players
        self.characters = characters
        self.alive_players: List[DialogAgent] = []
        self.werewolves: List[DialogAgent] = []
        self.seer: Optional[DialogAgent] = None
        self.witch: Optional[DialogAgent] = None
        self.villagers: List[DialogAgent] = []
        self.current_round = 0
        self.game_over = False
        self.winner = None
        self.moderator = Moderator()
    
    async def initialize(self):
        """初始化游戏状态和角色分配"""
        print("🎮 欢迎来到三国狼人杀！")
        print("\n=== 游戏初始化 ===")
        
        # 角色分配
        roles = ["狼人", "狼人", "预言家", "女巫", "村民", "村民"]
        
        # 创建智能体
        for i, (player, character) in enumerate(zip(self.players, self.characters)):
            role = roles[i]
            system_prompt = get_role_prompt(role, character)
            agent = DialogAgent(name=player, system_prompt=system_prompt)
            self.alive_players.append(agent)
            
            # 分配角色
            if role == "狼人":
                self.werewolves.append(agent)
            elif role == "预言家":
                self.seer = agent
            elif role == "女巫":
                self.witch = agent
            elif role == "村民":
                self.villagers.append(agent)
            
            # 通知玩家角色
            await self.moderator.notify(player, f"你在这场三国狼人杀中扮演{role}，你的角色是{character}。")
        
        print(f"游戏主持人: 📢 三国狼人杀游戏开始！参与者：{', '.join(self.players)}")
        print(f"✅ 游戏设置完成，共{len(self.players)}名玩家")
    
    async def run(self):
        """运行游戏主循环"""
        await self.initialize()
        
        while not self.game_over:
            self.current_round += 1
            print(f"\n=== 第{self.current_round}轮游戏 ===")
            
            # 夜晚阶段
            await self.night_phase()
            
            # 检查游戏是否结束
            if self.check_game_over():
                break
            
            # 白天阶段
            await self.day_phase()
            
            # 检查游戏是否结束
            if self.check_game_over():
                break
        
        # 宣布游戏结果
        await self.announce_result()
    
    async def night_phase(self):
        """夜晚阶段"""
        print("🌙 第1夜降临，天黑请闭眼...")
        
        # 狼人阶段
        await self.werewolf_phase()
        
        # 预言家阶段
        await self.seer_phase()
        
        # 女巫阶段
        await self.witch_phase()
    
    async def werewolf_phase(self):
        """狼人阶段"""
        if not self.werewolves:
            return
        
        print("\n【狼人阶段】")
        print("游戏主持人: 📢 🐺 狼人请睁眼，选择今晚要击杀的目标...")
        
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
                    try:
                        await wolf("请分析当前局势并表达你的观点。", structured_model=DiscussionModelCN)
                    except Exception as e:
                        print(f"⚠️ {wolf.name} 讨论时出错: {e}")
                        # 创建默认响应，确保游戏继续进行
                        default_response = DiscussionModelCN(
                            reach_agreement=False,
                            confidence_level=5,
                            key_evidence="暂时无法分析"
                        )
            
            # 投票阶段：收集并统计狼人的击杀决策
            werewolves_hub.set_auto_broadcast(False)
            kill_votes = await fanout_pipeline(
                self.werewolves,
                msg=await self.moderator.announce("请选择击杀目标"),
                structured_model=WerewolfKillModelCN,
                enable_gather=False,
            )
            
            # 统计投票结果
            vote_count = {}
            for vote in kill_votes:
                if hasattr(vote, "target_name"):
                    target = vote.target_name
                    vote_count[target] = vote_count.get(target, 0) + 1
            
            # 确定击杀目标
            if vote_count:
                self.killed_player = max(vote_count, key=vote_count.get)
                print(f"游戏主持人: 📢 狼人选择击杀 {self.killed_player}")
            else:
                self.killed_player = None
    
    async def seer_phase(self):
        """预言家阶段"""
        if not self.seer or self.seer not in self.alive_players:
            return
        
        print("\n【预言家阶段】")
        print("游戏主持人: 📢 🔮 预言家请睁眼，选择要查验的玩家...")
        
        # 预言家查验
        try:
            result = await self.seer(
                f"请选择一名玩家进行查验。存活玩家：{format_player_list(self.alive_players)}",
                structured_model=SeerActionModelCN
            )
            
            if hasattr(result, "target_name"):
                target = result.target_name
                # 检查目标是否为狼人
                is_werewolf = any(wolf.name == target for wolf in self.werewolves)
                print(f"{self.seer.name}: 我要查验{target}。")
                print(f"游戏主持人: 📢 查验结果：{target}是{'狼人' if is_werewolf else '好人'}")
        except Exception as e:
            print(f"⚠️ 预言家行动时出错: {e}")
    
    async def witch_phase(self):
        """女巫阶段"""
        if not self.witch or self.witch not in self.alive_players:
            return
        
        print("\n【女巫阶段】")
        print("游戏主持人: 📢 🧙‍♀️ 女巫请睁眼...")
        
        if self.killed_player:
            print(f"游戏主持人: 📢 今晚{self.killed_player}被狼人击杀")
            
            # 女巫行动
            try:
                result = await self.witch(
                    f"今晚{self.killed_player}被狼人击杀，你可以选择使用解药或毒药。",
                    structured_model=WitchActionModelCN
                )
                
                if hasattr(result, "use_antidote") and result.use_antidote:
                    print(f"{self.witch.name}: 我昨晚使用了解药救了{self.killed_player}，现在解药已经用掉了。")
                    print(f"游戏主持人: 📢 你使用解药救了{self.killed_player}")
                    self.killed_player = None
                elif hasattr(result, "use_poison") and result.use_poison and hasattr(result, "target_name"):
                    poison_target = result.target_name
                    print(f"{self.witch.name}: 我使用毒药毒死了{poison_target}。")
                    print(f"游戏主持人: 📢 你使用毒药毒死了{poison_target}")
                    # 移除被毒死的玩家
                    self.alive_players = [p for p in self.alive_players if p.name != poison_target]
            except Exception as e:
                print(f"⚠️ 女巫行动时出错: {e}")
        
        # 宣布夜晚结果
        if self.killed_player:
            print(f"游戏主持人: 📢 昨夜{self.killed_player}被狼人击杀。")
            # 移除被击杀的玩家
            self.alive_players = [p for p in self.alive_players if p.name != self.killed_player]
        else:
            print("游戏主持人: 📢 昨夜平安无事，无人死亡。")
    
    async def day_phase(self):
        """白天阶段"""
        print("\n【白天讨论阶段】")
        print("游戏主持人: 📢 ☀️ 第1天天亮了，请大家睁眼...")
        print(f"游戏主持人: 📢 现在开始自由讨论。存活玩家：{format_player_list(self.alive_players)}")
        
        # 自由讨论
        for _ in range(MAX_DISCUSSION_ROUND):
            for player in self.alive_players:
                try:
                    await player("请分析当前局势并表达你的观点。", structured_model=DiscussionModelCN)
                except Exception as e:
                    print(f"⚠️ {player.name} 讨论时出错: {e}")
        
        # 投票阶段
        print("\n【投票阶段】")
        print("游戏主持人: 📢 请投票选择要淘汰的玩家")
        
        # 并行收集所有玩家的投票决策
        vote_msgs = await fanout_pipeline(
            self.alive_players,
            await self.moderator.announce("请投票选择要淘汰的玩家"),
            structured_model=VoteModelCN,
            enable_gather=False,
        )
        
        # 统计投票结果
        vote_count = {}
        for i, vote in enumerate(vote_msgs):
            player = self.alive_players[i]
            if hasattr(vote, "target_name"):
                target = vote.target_name
                vote_count[target] = vote_count.get(target, 0) + 1
                print(f"{player.name}: 我选择投票给{target}。")
        
        # 确定被淘汰的玩家
        if vote_count:
            eliminated = max(vote_count, key=vote_count.get)
            print(f"游戏主持人: 📢 {eliminated} 被投票淘汰")
            # 移除被淘汰的玩家
            self.alive_players = [p for p in self.alive_players if p.name != eliminated]
        else:
            print("游戏主持人: 📢 投票无效，无人被淘汰")
    
    def check_game_over(self) -> bool:
        """检查游戏是否结束"""
        # 检查狼人是否全部死亡
        alive_werewolves = [w for w in self.werewolves if w in self.alive_players]
        if not alive_werewolves:
            self.game_over = True
            self.winner = "好人阵营"
            return True
        
        # 检查好人是否全部死亡
        alive_good = len(self.alive_players) - len(alive_werewolves)
        if alive_good <= 0:
            self.game_over = True
            self.winner = "狼人阵营"
            return True
        
        return False
    
    async def announce_result(self):
        """宣布游戏结果"""
        print(f"\n=== 游戏结束 ===")
        print(f"游戏主持人: 📢 游戏结束！获胜阵营是：{self.winner}")
        print(f"游戏主持人: 📢 存活玩家：{format_player_list(self.alive_players)}")
        print("🎮 三国狼人杀游戏圆满结束！")
