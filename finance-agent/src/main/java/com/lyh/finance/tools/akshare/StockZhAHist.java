package com.lyh.finance.tools.akshare;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/8 22:23
 */
@Slf4j
@Service
public class StockZhAHist extends BaseEmApi {
    private static final Object[] HEAD = new Object[]{"日期", "开盘", "收盘", "最高", "最低", "成交量", "成交额", "振幅(%)", "涨跌幅(%)", "涨跌额", "换手率(%)"};

    public StockZhAHist(RestTemplate restTemplate) {
        super(restTemplate);
    }

    /**
     * 获取沪深京 A 股历史行情
     * 直接返回二维字符串数组，对应 Python 代码中 DataFrame 的底层数据逻辑。
     *
     * @param symbol    股票代码 (例如: "000001", "603777")
     * @param period    周期: "daily", "weekly", "monthly"
     * @param startDate 开始日期: "yyyyMMdd"
     * @param endDate   结束日期: "yyyyMMdd"
     * @param adjust    复权: "qfq"(前复权), "hfq"(后复权), ""(不复权)
     * @param timeout   超时时间 (秒), 此处未动态设置 RestTemplate 超时，需在 Bean 配置中设定
     * @return String[][]
     * 列顺序: [日期, 股票代码, 开盘, 收盘, 最高, 最低, 成交量, 成交额, 振幅, 涨跌幅, 涨跌额, 换手率]
     * 如果无数据，返回长度为 0 的二维数组 (new String[0][12])
     */
    public Object[][] stockZhAHist(
            String symbol,
            String period,
            String startDate,
            String endDate,
            String adjust,
            Double timeout) {
        // 1. 参数映射逻辑
        String marketCode = symbol.startsWith("6") ? "1" : "0";

        Map<String, String> adjustDict = new HashMap<>();
        adjustDict.put("qfq", "1");
        adjustDict.put("hfq", "2");
        adjustDict.put("", "0");

        Map<String, String> periodDict = new HashMap<>();
        periodDict.put("daily", "101");
        periodDict.put("weekly", "102");
        periodDict.put("monthly", "103");

        if (!periodDict.containsKey(period)) {
            throw new IllegalArgumentException("Invalid period: " + period + ". Choose from daily, weekly, monthly.");
        }
        if (!adjustDict.containsKey(adjust)) {
            throw new IllegalArgumentException("Invalid adjust: " + adjust + ". Choose from qfq, hfq, or empty string.");
        }

        String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get";

        // 2. 构建请求 URL
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("fields1", "f1,f2,f3,f4,f5,f6")
                .queryParam("fields2", "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f116")
                .queryParam("ut", "7eea3edcaed734bea9cbfc24409ed989")
                .queryParam("klt", periodDict.get(period))
                .queryParam("fqt", adjustDict.get(adjust))
                .queryParam("secid", marketCode + "." + symbol)
                .queryParam("beg", startDate)
                .queryParam("end", endDate);

        URI uri = builder.build().toUri();
        HttpHeaders headers = buildHeaders("push2his.eastmoney.com");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        // 3. 发送请求
        ResponseEntity<String> response;
        try {
            // 注意：RestTemplate 的 timeout 通常在初始化时通过 ClientHttpRequestFactory 配置。
            response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Request failed for symbol: " + symbol, e);
        }
        if (response.getBody() == null) {
            return new String[0][12];
        }
        // 4. 解析 JSON
        try {
            JSONObject rootNode = JSONObject.parseObject(response.getBody());
            JSONObject dataNode = rootNode.getJSONObject("data");

            // 检查 data 节点是否存在
            if (dataNode == null || dataNode.isEmpty()) {
                return new String[0][12];
            }

            JSONArray klinesNode = dataNode.getJSONArray("klines");

            // 检查 klines 是否存在且为数组
            if (klinesNode == null || klinesNode.isEmpty()) {
                return new String[0][12];
            }

            int rowCount = Math.min(klinesNode.size(), maxNum);
            Object[][] result = new Object[rowCount + 1][HEAD.length];
            result[0] = HEAD;
            int i = 1;
            for (Object lineNode : klinesNode) {
                String line = (String) lineNode;
                // -1 保证保留末尾的空字符串，虽然东方财富通常没有
                String[] parts = line.split(",", -1);
                // 预期 parts 长度至少为 11 (索引 0-10)，对应 f51-f61
                if (parts.length < 11) {
                    i++;
                    continue;
                }
                for (int j = 0; j < HEAD.length; j++) {
                    result[i][j] = parts[j];
                }
                i++;
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("请求接口失败：" + uri, e);
        }
    }
}