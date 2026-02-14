from autogen_agentchat.teams import RoundRobinGroupChat
from autogen_agentchat.conditions import TextMentionTermination
from role_product_manager import create_product_manager
from role_engineer import create_engineer
from role_code_reviewer import create_code_reviewer
from role_user_proxy import create_user_proxy
from model_client import create_openai_model_client


def create_team_chat():
    model_client = create_openai_model_client()
    product_manager = create_product_manager(model_client)
    engineer = create_engineer(model_client)
    code_reviewer = create_code_reviewer(model_client)
    user_proxy = create_user_proxy()
    # 定义团队聊天和协作规则
    team_chat = RoundRobinGroupChat(
        participants=[
            product_manager,
            engineer,
            code_reviewer,
            user_proxy
        ],
        termination_condition=TextMentionTermination("TERMINATE"),
        max_turns=20,
    )
    return team_chat

