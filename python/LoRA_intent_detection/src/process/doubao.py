import csv
import random

from LoRA_intent_detection.src import config

# 定义各意图的模板和替换词
intent_templates = {
    "条件选股": [
        "{market};{industry}{indicator}{range};",
        "{industry}中{indicator}{range}的股票",
        "{market}里{indicator}{range}的{type}"
    ],
    "查询数据": [
        "{target}的{metric}",
        "{target}{metric}是多少",
        "查一下{target}的{metric}"
    ],
    "综合诊股": [
        "分析{stock}的综合情况",
        "{stock}综合诊股",
        "评估{stock}的整体表现"
    ],
    "综合诊基": [
        "{fund}的综合评估",
        "分析{fund}的综合情况",
        "{fund}综合诊基"
    ],
    "基本面诊股": [
        "{stock}的{finance}分析",
        "分析{stock}的{finance}",
        "{stock}基本面分析：{finance}"
    ],
    "技术面诊股": [
        "{stock}的{tech}分析",
        "分析{stock}的{tech}指标",
        "{stock}技术面：{tech}"
    ],
    "消息面诊股": [
        "{stock}的{news}解读",
        "分析{stock}的{news}影响",
        "{stock}消息面：{news}"
    ],
    "涨跌分析": [
        "{target}今日涨跌原因分析",
        "分析{target}涨跌的因素",
        "{target}为什么涨/跌"
    ],
    "自选股分析": [
        "我的自选股池里的{stock}分析",
        "自选股{stock}本周表现分析",
        "分析我的自选{plate}股票"
    ],
    "知识问答": [
        "{term}是什么意思",
        "{term}和{term2}的区别",
        "怎么看{term}指标"
    ]
}

# 替换词库
replace_words = {
    "market": ["A股", "港股", "美股"],
    "industry": ["半导体", "白酒", "新能源", "医药", "金融", "消费"],
    "indicator": ["市净率", "成交量", "涨幅", "市盈率", "换手率"],
    "range": ["0-5", "最大3个", "近1月", "近3月", "最小5个"],
    "type": ["股票", "基金", "债券"],
    "target": ["贵州茅台", "宁德时代", "比亚迪", "黄金etf", "上证指数"],
    "metric": ["涨幅", "股东增长率", "主力资金净流入", "市盈率", "市净率"],
    "stock": ["贵州茅台", "宁德时代", "比亚迪", "隆基绿能", "招商银行"],
    "fund": ["易方达蓝筹精选混合", "华夏新能源混合", "南方债券基金", "嘉实沪深300ETF"],
    "finance": ["营收", "净利润", "资产负债率", "毛利率", "净利率"],
    "tech": ["K线形态", "MACD", "均线", "成交量", "KDJ"],
    "news": ["政策利好", "业绩预告", "股东增减持", "行业新闻"],
    "plate": ["白酒", "新能源", "半导体", "医药"],
    "term": ["市净率", "市盈率", "ETF", "北向资金", "均线"],
}

# 生成5000条数据（每个意图500条）
data = []
header = ["input", "output"]
for intent, templates in intent_templates.items():
    for _ in range(500):
        # 随机选一个模板
        template = random.choice(templates)
        # 替换模板中的变量
        for key in replace_words:
            if "{" + key + "}" in template:
                if key == "term2":  # 知识问答的第二个术语
                    template = template.replace("{term2}", random.choice(replace_words["term"]))
                else:
                    template = template.replace("{" + key + "}", random.choice(replace_words[key]))
        # 构造数据行
        data.append([
            template,
            intent
        ])

# 写入CSV文件
with open(config.PROCESSED_DATA_DIR / "doubao_finance_intent_5000.csv", "w", encoding="utf-8", newline="") as f:
    writer = csv.writer(f)
    writer.writerow(header)
    writer.writerows(data)

print("5000条金融意图数据集已生成：doubao_finance_intent_5000.csv")
