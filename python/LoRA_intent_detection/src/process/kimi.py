import pandas as pd
import random
from datetime import datetime, timedelta

from LoRA_intent_detection.src import config

# 设置随机种子保证可重复性
random.seed(42)

# 定义各类实体和模板
stocks = [
    "贵州茅台", "宁德时代", "比亚迪", "招商银行", "中国平安", "五粮液", "隆基绿能",
    "中信证券", "东方财富", "药明康德", "恒瑞医药", "迈瑞医疗", "海康威视", "美的集团",
    "立讯精密", "顺丰控股", "爱尔眼科", "京东方A", "牧原股份", "立讯精密", "伊利股份",
    "格力电器", "平安银行", "兴业银行", "浦发银行", "建设银行", "工商银行", "农业银行",
    "中国石油", "中国石化", "中国中免", "紫金矿业", "万华化学", "三一重工", "通威股份",
    "长江电力", "中国神华", "山西汾酒", "泸州老窖", "洋河股份", "古井贡酒", "片仔癀",
    "云南白药", "长春高新", "智飞生物", "复星医药", "沃森生物", "凯莱英", "泰格医药",
    "康龙化成", "北方华创", "中芯国际", "韦尔股份", "紫光国微", "兆易创新", "圣邦股份",
    "卓胜微", "三安光电", "闻泰科技", "长电科技", "通富微电", "华天科技", "士兰微",
    "中微公司", "澜起科技", "沪硅产业", "华润微", "中芯集成", "华虹公司"
]

industries = [
    "半导体", "白酒", "新能源", "银行", "证券", "保险", "医药", "医疗器械", "电子",
    "计算机", "通信", "传媒", "军工", "电力", "化工", "机械", "汽车", "家电",
    "食品饮料", "农林牧渔", "房地产", "建筑", "建材", "钢铁", "煤炭", "有色金属",
    "石油石化", "基础化工", "交通运输", "商贸零售", "社会服务", "美容护理", "纺织服饰",
    "轻工制造", "医药生物", "公用事业", "环保", "综合", "机器人", "人工智能",
    "光伏", "锂电池", "储能", "风电", "核电", "充电桩", "车联网", "物联网",
    "5G", "芯片", "集成电路", "消费电子", "面板", "LED", "云计算", "大数据",
    "网络安全", "金融科技", "区块链", "数字货币", "元宇宙", "虚拟现实", "增强现实"
]

funds = [
    "招商中证白酒指数", "易方达蓝筹精选", "华夏能源革新", "中欧医疗健康", "景顺长城新兴成长",
    "兴全合润", "富国天惠成长", "易方达消费行业", "汇添富消费行业", "广发双擎升级",
    "嘉实智能汽车", "银华富裕主题", "南方中证500ETF", "华夏上证50ETF", "华泰柏瑞沪深300ETF",
    "天弘余额宝", "博时黄金ETF", "华安黄金ETF", "易方达黄金ETF", "国泰黄金ETF",
    "中概互联网ETF", "纳斯达克100指数基金", "标普500指数基金", "恒生科技ETF", "中证医疗ETF",
    "光伏ETF", "新能源车ETF", "芯片ETF", "5G通信ETF", "人工智能ETF"
]

indicators = [
    "市盈率", "市净率", "市销率", "股息率", "ROE", "ROA", "毛利率", "净利率",
    "营收增长率", "净利润增长率", "资产负债率", "流动比率", "速动比率", "现金流量",
    "换手率", "成交量", "成交额", "振幅", "涨跌幅", "主力资金净流入", "北向资金",
    "融资余额", "融券余量", "股东人数", "户均持股", "机构持仓", "基金持仓"
]

time_periods = [
    "近1月", "近3月", "近6月", "近1年", "近2年", "近3年", "近5年", "今年来",
    "近1周", "近5日", "近10日", "近20日", "近60日", "近120日", "近250日",
    "本月", "本季度", "本年度", "上市以来", "成立以来"
]

data_types = [
    "涨跌幅", "涨幅", "跌幅", "价格", "收盘价", "开盘价", "最高价", "最低价",
    "成交量", "成交额", "换手率", "振幅", "主力资金净流入", "北向资金净流入",
    "融资买入额", "融券卖出量", "股东增长率", "机构持仓变化", "基金持仓变化",
    "业绩预告", "业绩快报", "年报", "中报", "季报", "营收", "净利润", "扣非净利润"
]


def generate_intent_1_condition_stock():
    """条件选股"""
    templates = [
        "A股;东财二级行业包含{industry}市净率{pb_low}-{pb_high};",
        "A股;东财三级行业包含{industry}市盈率{pe_low}-{pe_high};",
        "{industry}板块{indicator}最大的{num}个股票",
        "{industry}板块{indicator}最小的{num}个股票",
        "A股;连续{days}日{indicator}大于{value}的股票",
        "A股;市值{min_val}亿-{max_val}亿;{indicator}大于{val};",
        "{industry}板块;主力净流入连续{days}日大于{val}万;",
        "A股;ROE连续{years}年大于{roe_val};{indicator}小于{val};",
        "{industry}板块;近{period}涨幅{cond}{pct}%;{indicator}范围{low}-{high};",
        "A股;股息率大于{dvd}%;市净率小于{pb};",
        "{industry}板块;{time}换手率大于{turnover}%;",
        "A股;北向资金持股占比大于{ratio}%;{indicator}小于{val};",
        "A股;连续{days}日涨停;{indicator}小于{val};",
        "{industry}板块;净利润增长率大于{growth}%;{indicator}小于{val};",
        "A股;机构评级买入家数大于{buy_num};{indicator}小于{val};"
    ]

    template = random.choice(templates)
    data = {
        "industry": random.choice(industries),
        "pb_low": random.randint(0, 5),
        "pb_high": random.randint(5, 20),
        "pe_low": random.randint(0, 20),
        "pe_high": random.randint(20, 100),
        "num": random.choice([3, 5, 10, 20, 50]),
        "days": random.randint(3, 20),
        "years": random.randint(3, 5),
        "value": round(random.uniform(1000, 10000), 2),
        "min_val": random.randint(50, 500),
        "max_val": random.randint(500, 5000),
        "val": round(random.uniform(10, 100), 2),
        "roe_val": round(random.uniform(10, 30), 2),
        "period": random.choice(["1月", "3月", "6月"]),
        "cond": random.choice([">", "<", ">=", "<="]),
        "pct": random.randint(10, 50),
        "low": round(random.uniform(0.5, 10), 2),
        "high": round(random.uniform(10, 100), 2),
        "dvd": round(random.uniform(2, 8), 2),
        "pb": round(random.uniform(1, 5), 2),
        "time": random.choice(["近1月", "近3月", "近1周"]),
        "turnover": random.randint(5, 30),
        "ratio": round(random.uniform(1, 10), 2),
        "growth": random.randint(20, 100),
        "buy_num": random.randint(5, 30),
        "indicator": random.choice(indicators)
    }
    return template.format(**data)


def generate_intent_2_query_data():
    """查询数据"""
    templates = [
        "{stock}{data_type}多少",
        "{stock}的{data_type}",
        "{stock}{time}{data_type}",
        "{stock}最新{data_type}",
        "查询{stock}{data_type}",
        "{stock}{data_type}数据",
        "{stock}{time}涨跌情况",
        "{stock}今日{data_type}",
        "{stock}主力资金流向",
        "{stock}北向资金持股",
        "{stock}融资融券数据",
        "{stock}股东户数变化",
        "{fund}{time}{data_type}",
        "{fund}最新净值",
        "{fund}规模变化",
        "黄金ETF主力资金净流入",
        "北向资金今日净流入",
        "两市今日成交额",
        "上证指数今日涨跌"
    ]

    template = random.choice(templates)
    data = {
        "stock": random.choice(stocks),
        "fund": random.choice(funds),
        "data_type": random.choice(data_types),
        "time": random.choice(time_periods)
    }
    return template.format(**data)


def generate_intent_3_comprehensive_stock():
    """综合诊股"""
    templates = [
        "诊断{stock}",
        "{stock}怎么样",
        "分析{stock}",
        "{stock}能买吗",
        "{stock}后市如何",
        "{stock}投资价值分析",
        "{stock}技术面和基本面分析",
        "{stock}综合评价",
        "{stock}诊断报告",
        "{stock}体检报告",
        "{stock}健康状况",
        "{stock}值得投资吗",
        "{stock}风险评估",
        "{stock}持仓建议",
        "{stock}操作策略"
    ]
    return random.choice(templates).format(stock=random.choice(stocks))


def generate_intent_4_comprehensive_fund():
    """综合诊基"""
    templates = [
        "诊断{fund}",
        "{fund}怎么样",
        "分析{fund}",
        "{fund}能买吗",
        "{fund}后市如何",
        "{fund}投资价值分析",
        "{fund}综合评价",
        "{fund}诊断报告",
        "{fund}体检报告",
        "{fund}值得投资吗",
        "{fund}风险评估",
        "{fund}持仓建议",
        "{fund}操作策略",
        "{fund}业绩评价",
        "{fund}基金经理分析"
    ]
    return random.choice(templates).format(fund=random.choice(funds))


def generate_intent_5_fundamental():
    """基本面诊股"""
    templates = [
        "{stock}基本面分析",
        "{stock}财务状况",
        "{stock}盈利能力分析",
        "{stock}偿债能力分析",
        "{stock}成长性分析",
        "{stock}现金流分析",
        "{stock}资产负债表分析",
        "{stock}利润表分析",
        "{stock}现金流量表分析",
        "{stock}ROE分析",
        "{stock}毛利率变化",
        "{stock}营收增长情况",
        "{stock}净利润质量",
        "{stock}财务风险",
        "{stock}同业财务对比"
    ]
    return random.choice(templates).format(stock=random.choice(stocks))


def generate_intent_6_technical():
    """技术面诊股"""
    templates = [
        "{stock}技术分析",
        "{stock}K线形态",
        "{stock}趋势分析",
        "{stock}支撑位和压力位",
        "{stock}均线系统",
        "{stock}MACD指标",
        "{stock}KDJ指标",
        "{stock}RSI指标",
        "{stock}布林带分析",
        "{stock}成交量分析",
        "{stock}量价关系",
        "{stock}技术形态",
        "{stock}短线技术信号",
        "{stock}中长线技术走势",
        "{stock}技术指标综合"
    ]
    return random.choice(templates).format(stock=random.choice(stocks))


def generate_intent_7_news():
    """消息面诊股"""
    templates = [
        "{stock}最新消息",
        "{stock}公告解读",
        "{stock}新闻舆情",
        "{stock}利好利空",
        "{stock}机构评级",
        "{stock}研报观点",
        "{stock}龙虎榜分析",
        "{stock}大宗交易",
        "{stock}股东增减持",
        "{stock}融资融券变化",
        "{stock}北向资金动向",
        "{stock}主力资金流向",
        "{stock}市场情绪",
        "{stock}热点概念",
        "{stock}催化剂分析"
    ]
    return random.choice(templates).format(stock=random.choice(stocks))


def generate_intent_8_price_analysis():
    """涨跌分析"""
    templates = [
        "{stock}为什么涨跌",
        "{stock}涨跌原因",
        "{stock}后市预测",
        "{stock}能否继续上涨",
        "{stock}下跌原因分析",
        "{stock}反弹力度分析",
        "{stock}调整到位了吗",
        "{stock}还有上涨空间吗",
        "{stock}支撑位在哪",
        "{stock}压力位在哪",
        "{stock}目标价",
        "{stock}止损位",
        "{stock}涨跌空间测算",
        "{stock}短线走势预判",
        "{stock}中线趋势判断"
    ]
    return random.choice(templates).format(stock=random.choice(stocks))


def generate_intent_9_watchlist():
    """自选股分析"""
    templates = [
        "分析我的自选股",
        "自选股组合诊断",
        "我的股票池分析",
        "持仓股票分析",
        "自选股风险评估",
        "自选股配置建议",
        "自选股行业分布",
        "自选股收益分析",
        "自选股今日表现",
        "自选股资金流向",
        "自选股消息面",
        "自选股操作建议",
        "自选股调仓建议",
        "自选股盈亏分析",
        "自选股轮动策略"
    ]
    return random.choice(templates)


def generate_intent_10_knowledge():
    """知识问答"""
    templates = [
        "什么是{concept}？",
        "{concept}是什么意思？",
        "如何理解{concept}？",
        "{concept}的计算公式",
        "{concept}怎么看？",
        "{concept}怎么算？",
        "{concept}的标准范围",
        "{concept}高好还是低好？",
        "什么是{industry}行业？",
        "{industry}行业特点",
        "{indicator}指标解读",
        "股票术语{concept}解释",
        "基金术语{concept}解释",
        "K线形态{concept}介绍",
        "技术分析{concept}原理"
    ]

    concepts = ["市盈率", "市净率", "ROE", "毛利率", "换手率", "振幅", "成交量", "量比",
                "委比", "委差", "内外盘", "换手率", "复权", "除权", "除息", "股息率",
                "贝塔系数", "夏普比率", "最大回撤", "阿尔法", "波动率", "久期", "凸性",
                "MACD", "KDJ", "RSI", "BOLL", "MA", "EXPMA", "成交量", "换手率",
                "基本面", "技术面", "消息面", "资金面", "政策面", "情绪面",
                "多头", "空头", "牛市", "熊市", "震荡市", "反弹", "回调", "反转"]

    template = random.choice(templates)
    data = {
        "concept": random.choice(concepts),
        "industry": random.choice(industries),
        "indicator": random.choice(indicators)
    }
    return template.format(**data)


# 生成数据
all_data = []
intent_functions = [
    ("条件选股", generate_intent_1_condition_stock),
    ("查询数据", generate_intent_2_query_data),
    ("综合诊股", generate_intent_3_comprehensive_stock),
    ("综合诊基", generate_intent_4_comprehensive_fund),
    ("基本面诊股", generate_intent_5_fundamental),
    ("技术面诊股", generate_intent_6_technical),
    ("消息面诊股", generate_intent_7_news),
    ("涨跌分析", generate_intent_8_price_analysis),
    ("自选股分析", generate_intent_9_watchlist),
    ("知识问答", generate_intent_10_knowledge)
]

# 每类意图生成500条（共5000条）
samples_per_intent = 500

print("开始生成数据...")
for intent_name, func in intent_functions:
    print(f"正在生成: {intent_name}...")
    for _ in range(samples_per_intent):
        text = func()
        all_data.append({
            "input": text,
            "output": intent_name
        })

# 打乱数据
random.shuffle(all_data)

# 创建DataFrame
df = pd.DataFrame(all_data)
df.to_csv(config.PROCESSED_DATA_DIR / 'kimi_finance_intent_5000.csv', index=False)

# 显示数据统计
print("\n数据生成完成！")
print(f"总样本数: {len(df)}")
print("\n各意图分布:")
print(df['output'].value_counts())
print("\n前10条样本预览:")
print(df.head(10))
