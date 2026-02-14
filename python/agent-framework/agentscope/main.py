import asyncio
from game import ThreeKingdomsWerewolfGame


async def main():
    """主函数"""
    # 定义玩家和对应的三国人物
    players = ["孙权", "周瑜", "曹操", "张飞", "司马懿", "赵云"]
    characters = ["孙权", "周瑜", "曹操", "张飞", "司马懿", "赵云"]
    
    # 创建游戏实例
    game = ThreeKingdomsWerewolfGame(players=players, characters=characters)
    
    # 运行游戏
    await game.run()


if __name__ == "__main__":
    asyncio.run(main())
