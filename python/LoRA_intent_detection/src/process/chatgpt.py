import csv
import random
from LoRA_intent_detection.src import config

intents = [
    "条件选股",
    "查询数据",
    "综合诊股",
    "综合诊基",
    "基本面诊股",
    "技术面诊股",
    "消息面诊股",
    "涨跌分析",
    "自选股分析",
    "知识问答"
]

stocks = ["贵州茅台", "宁德时代", "比亚迪", "中芯国际", "立讯精密", "东方财富", "隆基绿能", "五粮液", "海康威视"]
sectors = ["半导体", "白酒", "新能源", "机器人", "人工智能", "光伏", "军工", "银行"]
funds = ["易方达蓝筹精选", "华夏上证50ETF", "南方中证500ETF", "招商中证白酒基金"]
periods = ["近1月", "近3月", "近半年", "今年以来"]
metrics = ["市盈率", "市净率", "成交量", "涨幅", "主力资金净流入", "股东增长率"]
tech_indicators = ["MACD", "KDJ", "RSI", "均线多头排列"]


def generate_query(intent):
    stock = random.choice(stocks)
    sector = random.choice(sectors)
    fund = random.choice(funds)
    period = random.choice(periods)
    metric = random.choice(metrics)
    tech = random.choice(tech_indicators)

    if intent == "条件选股":
        return f"A股; {sector}板块; {metric} 0-30"
    elif intent == "查询数据":
        return f"{stock}{metric}"
    elif intent == "综合诊股":
        return f"{stock}现在适合持有吗"
    elif intent == "综合诊基":
        return f"{fund}现在能买吗"
    elif intent == "基本面诊股":
        return f"{stock}基本面怎么样"
    elif intent == "技术面诊股":
        return f"{stock}{tech}怎么看"
    elif intent == "消息面诊股":
        return f"{stock}最近有什么利好消息"
    elif intent == "涨跌分析":
        return f"{stock}{period}{metric}分析"
    elif intent == "自选股分析":
        return f"帮我分析自选股{stock}"
    elif intent == "知识问答":
        return f"什么是{metric}"
    else:
        return "未知问题"


with open(config.PROCESSED_DATA_DIR / "chatgpt_finance_intent_5000.csv", "w", newline='', encoding="utf-8-sig") as f:
    writer = csv.writer(f)
    writer.writerow(["input", "output"])

    id_counter = 1
    for intent in intents:
        for _ in range(500):
            writer.writerow([generate_query(intent), intent])
            id_counter += 1

print("已生成 5000 条数据 -> chatgpt_finance_intent_5000.csv")
