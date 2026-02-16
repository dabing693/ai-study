package com.lyh.finance.tools;

import com.lyh.base.agent.annotation.Tool;
import com.lyh.base.agent.util.MarkdownUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AkShare 金融数据工具
 */
@Service
public class AkShareTool {

    @Autowired
    private RestTemplate restTemplate;

    private static final String BASE_URL = "http://127.0.0.1:8089/api/public/";

    @Tool(description = "查询当前 A 股市场最热门的股票排行榜（实时热榜）")
    public String getHotStocks() {
        return MarkdownUtil.arrayToMarkdownTable(getHotStocksRaw());
    }

    public Object[][] getHotStocksRaw() {
        String url = BASE_URL + "stock_hot_rank_em";
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> dataList = response.getBody();
            if (dataList == null || dataList.isEmpty()) return new Object[][]{};

            int limit = Math.min(dataList.size(), 20);
            Object[][] table = new Object[limit + 1][6];
            table[0] = new Object[]{"排名", "代码", "名称", "最新价", "涨跌额", "涨跌幅(%)"};
            for (int i = 0; i < limit; i++) {
                Map<String, Object> item = dataList.get(i);
                table[i + 1] = new Object[]{item.get("当前排名"), item.get("代码"), item.get("股票名称"), item.get("最新价"), item.get("涨跌额"), item.get("涨跌幅")};
            }
            return table;
        } catch (Exception e) { return new Object[][]{{"错误", e.getMessage()}}; }
    }

    @Tool(description = "查询当前 A 股市场最热门的行业板块（实时行业行情）")
    public String getHotIndustries() {
        return MarkdownUtil.arrayToMarkdownTable(getHotIndustriesRaw());
    }

    public Object[][] getHotIndustriesRaw() {
        String url = BASE_URL + "stock_board_industry_name_em";
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> dataList = response.getBody();
            if (dataList == null || dataList.isEmpty()) return new Object[][]{};

            int limit = Math.min(dataList.size(), 15);
            Object[][] table = new Object[limit + 1][7];
            table[0] = new Object[]{"排名", "板块名称", "涨跌幅(%)", "上涨家数", "下跌家数", "领涨股票", "领涨跌幅(%)"};
            for (int i = 0; i < limit; i++) {
                Map<String, Object> item = dataList.get(i);
                table[i + 1] = new Object[]{item.get("排名"), item.get("板块名称"), item.get("涨跌幅"), item.get("上涨家数"), item.get("下跌家数"), item.get("领涨股票"), item.get("领涨股票-涨跌幅")};
            }
            return table;
        } catch (Exception e) { return new Object[][]{{"错误", e.getMessage()}}; }
    }

    @Tool(description = "查询最近一个交易日的龙虎榜数据（大资金动向）")
    public String getLhbData() {
        return MarkdownUtil.arrayToMarkdownTable(getLhbDataRaw());
    }

    public Object[][] getLhbDataRaw() {
        String urlTemplate = BASE_URL + "stock_lhb_detail_em?start_date=%s&end_date=%s";
        LocalDate date = LocalDate.now();
        List<Map<String, Object>> dataList = null;
        String actualDate = null;

        for (int i = 1; i <= 7; i++) {
            String formattedDate = date.minusDays(i).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            try {
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                        urlTemplate.formatted(formattedDate, formattedDate), HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );
                dataList = response.getBody();
                if (dataList != null && !dataList.isEmpty()) {
                    actualDate = formattedDate;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (dataList == null || dataList.isEmpty()) return new Object[][]{{"状态", "暂无龙虎榜数据"}};

        int limit = Math.min(dataList.size(), 15);
        Object[][] table = new Object[limit + 1][8];
        table[0] = new Object[]{"日期", "代码", "名称", "上榜原因", "买入额(万)", "卖出额(万)", "净买入(万)", "换手率(%)"};
        String displayDate = actualDate != null ? 
            actualDate.substring(0, 4) + "-" + actualDate.substring(4, 6) + "-" + actualDate.substring(6, 8) : "";
        for (int i = 0; i < limit; i++) {
            Map<String, Object> item = dataList.get(i);
            table[i + 1] = new Object[]{displayDate, item.get("代码"), item.get("名称"), item.get("上榜原因"), item.get("买入额"), item.get("卖出额"), item.get("净买入额"), item.get("换手率")};
        }
        return table;
    }
}
