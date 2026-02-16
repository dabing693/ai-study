import csv
import random

from LoRA_intent_detection.src import config

# --- 1. 定义实体池 (Entity Pools) ---
stocks = ["贵州茅台", "宁德时代", "比亚迪", "中国平安", "五粮液", "招商银行", "中芯国际", "工业富联", "药明康德",
          "东方财富", "隆基绿能", "海康威视", "长江电力", "中信证券", "格力电器"]
industries = ["半导体", "白酒", "机器人", "新能源车", "光伏", "医疗器械", "人工智能", "煤炭", "房地产", "军工",
              "计算机二级行业", "东财三级行业"]
indicators = ["市盈率", "市净率", "净利润增长率", "换手率", "成交量", "主力资金净流入", "股东增长率", "ROE",
              "资产负债率"]
funds = ["易方达蓝筹精选", "中欧医疗健康", "黄金ETF", "纳指100ETF", "中证500ETF", "南方原油", "债券基金"]
time_periods = ["近1月", "近3月", "今年以来", "近一年", "昨日", "最近一周"]
comparisons = ["最大", "最小", "最高", "最低", "前10名"]

# --- 2. 定义意图模板 (Intent Templates) ---
templates = {
    "1、条件选股": [
        "A股;{industry}包含{sub_industry}市净率0-5;",
        "{industry}板块{indicator}{comparison}的{n}个股票",
        "{time_period}涨幅最大的{fund_type}",
        "股价低于20元且{indicator}大于10%的{industry}股"
    ],
    "2、查询数据": [
        "{stock}涨了多少",
        "{stock}的{indicator}",
        "{fund}主力资金净流入",
        "{stock}最新的财报数据"
    ],
    "3、综合诊股": ["综合分析下{stock}", "帮我诊股{stock}", "{stock}现在能买吗", "深度评价一下{stock}的投资价值"],
    "4、综合诊基": ["{fund}表现怎么样", "诊基：{fund}", "{fund}目前的风险高吗", "帮我分析下{fund}这只基金"],
    "5、基本面诊股": ["{stock}的分红能力强吗", "{stock}的财务状况健康吗", "{stock}的营收构成分析",
                     "看下{stock}的盈利能力"],
    "6、技术面诊股": ["{stock}的技术指标如何", "看下{stock}的支撑位和压力位", "{stock}现在是多头排列吗",
                     "{stock}的K线走势分析"],
    "7、消息面诊股": ["{stock}最近有什么利空消息", "{stock}有重大利好吗", "分析下关于{stock}的新闻",
                     "最近政策对{industry}有什么影响"],
    "8、涨跌分析": ["为什么今天{industry}板块大跌", "解释一下{stock}放量上涨的原因", "大盘今天为什么跳水",
                   "创业板指上涨动力是什么"],
    "9自选股分析": ["看下我自选股里的{stock}", "我持仓的{industry}股最近风险大吗", "帮我诊断下我的自选股"],
    "10、知识问答": ["什么是{knowledge}", "怎么看{indicator}", "{indicator}的定义是什么", "股市里的{knowledge}怎么理解"]
}

knowledge_pool = ["量比", "MACD金叉", "除权除息", "换手率", "蓝筹股", "黑天鹅", "头肩底", "北向资金"]

# --- 3. 执行生成逻辑 ---
data_rows = []
total_count = 5000

for i in range(total_count):
    intent = random.choice(list(templates.keys()))
    tpl = random.choice(templates[intent])

    # 随机填充模板
    query = tpl.format(
        industry=random.choice(industries),
        sub_industry=random.choice(industries),
        stock=random.choice(stocks),
        indicator=random.choice(indicators),
        fund=random.choice(funds),
        fund_type=random.choice(["股票型基金", "债券基金", "混合型基金"]),
        time_period=random.choice(time_periods),
        comparison=random.choice(comparisons),
        n=random.randint(3, 10),
        knowledge=random.choice(knowledge_pool)
    )

    data_rows.append([query, intent])

# --- 4. 写入CSV ---
file_path = config.PROCESSED_DATA_DIR / 'gemini_finance_intent_5000.csv'
with open(file_path, 'w', encoding='utf-8-sig', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(["input", "output"])
    writer.writerows(data_rows)

print(f"成功生成 {total_count} 条数据至 {file_path}")
