package com.lyh.trade.tools;


import com.lyh.common.util.MarkdownUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * @author lengYinHui
 * @date 2026/1/23
 */
@Service
public class AccountService {
    /**
     * 1、资产分布信息
     * 入参：资金账号
     * 出参：实时总资产、现金、股票、场内ETF、天天宝（货币基金）、理财资产各项所占的仓位
     */
    @Tool(description = "获取资产分布信息")
    public String getAssetDistribution(@ToolParam(description = "资金账号") String accountId) {
        String[][] assetData = {
                {"资产类型", "金额", "占比"},
                {"实时总资产", "100000元", "100%"},
                {"现金", "20000元", "20%"},
                {"股票", "50000元", "50%"},
                {"场内ETF", "10000元", "10%"},
                {"天天宝（货币基金）", "5000元", "5%"},
                {"理财资产", "5000元", "5%"}
        };
        return MarkdownUtil.arrayToMarkdownTable(assetData);
    }

    /**
     * 2、股票持仓信息
     * 入参：资金账号
     * 出参：持仓列表，每个股票信息包括股票名称、持仓收益率、价格、成本价、行业、持仓天数
     */
    @Tool(description = "获取股票持仓信息")
    public String getStockHoldings(@ToolParam(description = "资金账号") String accountId) {
        String[][] stockData = {
                {"股票名称", "持仓收益率", "价格", "成本价", "行业", "持仓天数"},
                {"贵州茅台", "10.5%", "1800.00元", "1630.00元", "白酒", "30天"},
                {"宁德时代", "-5.2%", "250.00元", "263.60元", "新能源", "15天"}
        };
        return MarkdownUtil.arrayToMarkdownTable(stockData);
    }

    /**
     * 3、账户收益信息
     * 入参：资金账号
     * 出参：包括多个区间（当日收益、本周收益、本月收益、今年收益）的数据（收益率、收益金额、收益构成（收益top3股票、亏损top3股票）、最大回撤、同期沪深300）
     */
    @Tool(description = "获取账户收益信息")
    public String getAccountProfit(@ToolParam(description = "资金账号") String accountId) {
        String[][] profitData = {
                {"区间", "收益率", "收益金额", "收益构成", "最大回撤", "同期沪深300"},
                {"当日收益", "0.5%", "500元", "收益top3：贵州茅台（+300元）<br>宁德时代（+150元）<br>比亚迪（+100元）<br>亏损top3：腾讯控股（-80元）<br>阿里巴巴（-50元）<br>美团（-20元）", "2.0%", "0.3%"},
                {"本周收益", "2.8%", "2800元", "收益top3：宁德时代（+1000元）<br>比亚迪（+800元）<br>贵州茅台（+600元）<br>亏损top3：阿里巴巴（-200元）<br>腾讯控股（-150元）<br>京东（-100元）", "3.2%", "1.5%"},
                {"本月收益", "6.2%", "6200元", "收益top3：比亚迪（+2000元）<br>宁德时代（+1800元）<br>贵州茅台（+1500元）<br>亏损top3：美团（-300元）<br>京东（-200元）<br>腾讯控股（-150元）", "4.8%", "2.8%"},
                {"今年收益", "18.5%", "18500元", "收益top3：宁德时代（+6000元）<br>比亚迪（+5000元）<br>贵州茅台（+4000元）<br>亏损top3：腾讯控股（-800元）<br>美团（-600元）<br>阿里巴巴（-400元）", "9.5%", "7.2%"}
        };
        return MarkdownUtil.arrayToMarkdownTable(profitData);
    }

    /**
     * 4、账户交易信息
     * 入参：资金账号、股票代码、区间（）
     * 出参：交易列表，每个标的包括标的名称、交易信息、做T收益
     */
    @Tool(description = "获取账户交易信息")
    public String getAccountTransactions(
            @ToolParam(description = "资金账号") String accountId,
            @ToolParam(description = "股票代码") String stockCode,
            @ToolParam(description = "交易区间") String period) {
        String[][] transactionData = {
                {"标的名称", "交易信息", "做T收益"},
                {"贵州茅台", "2026-01-20 买入 100股 1750.00元", "5000元"},
                {"宁德时代", "2026-01-18 卖出 50股 255.00元", "-250元"}
        };
        return MarkdownUtil.arrayToMarkdownTable(transactionData);
    }
}
