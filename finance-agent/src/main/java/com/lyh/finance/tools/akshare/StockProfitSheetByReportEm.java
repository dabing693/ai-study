package com.lyh.finance.tools.akshare;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/8 23:55
 */
@Slf4j
@Service
public class StockProfitSheetByReportEm extends BaseEmApi {
    public StockProfitSheetByReportEm(RestTemplate restTemplate) {
        super(restTemplate);
    }

    /**
     * 获取东方财富-股票-财务分析-利润表-报告期
     * 对应 Python: ak.stock_profit_sheet_by_report_em
     * 接口地址: https://emweb.securities.eastmoney.com/PC_HSF10/NewFinanceAnalysis/lrbDateAjaxNew
     *
     * @param symbol 股票代码 (带市场标识，如 "SH600519", "SZ000001", "BJ83xxxx")
     * @return Object[][]
     * 第一行为表头，后续为数据行。如果无数据返回 new Object[0][0]
     */
    public Object[][] stockProfitSheetByReportEm(String symbol) {
        String companyType = getCompanyType(symbol);
        String baseUrlDate = "https://emweb.securities.eastmoney.com/PC_HSF10/NewFinanceAnalysis/lrbDateAjaxNew";
        String baseUrlData = "https://emweb.securities.eastmoney.com/PC_HSF10/NewFinanceAnalysis/lrbAjaxNew";

        // 2. 第一步：获取所有报告日期
        UriComponentsBuilder dateBuilder = UriComponentsBuilder.fromHttpUrl(baseUrlDate)
                .queryParam("companyType", companyType)
                .queryParam("reportDateType", "0") // 0: 报告期
                .queryParam("code", symbol);
        List<String> reportDates;
        try {
            String dateUrl = dateBuilder.toUriString();
            ResponseEntity<String> response = restTemplate.getForEntity(dateUrl, String.class);
            if (response.getBody() == null) {
                return new Object[0][0];
            }
            JSONObject root = JSONObject.parseObject(response.getBody());
            JSONArray dataArr = root.getJSONArray("data");
            if (dataArr == null || dataArr.isEmpty()) {
                return new Object[0][0];
            }
            //提取 REPORT_DATE字段
            reportDates = new ArrayList<>();
            for (int i = 0; i < dataArr.size(); i++) {
                JSONObject item = dataArr.getJSONObject(i);
                if (item.containsKey("REPORT_DATE")) {
                    String dateStr = item.getString("REPORT_DATE");
                    if (dateStr != null && dateStr.length() > 10) {
                        dateStr = dateStr.substring(0, 10);
                    }
                    reportDates.add(dateStr);
                }
            }
        } catch (Exception e) {
            log.error("获取报告期失败：{}", symbol, e);
            throw new RuntimeException("获取报告期失败：" + symbol, e);
        }
        if (reportDates.isEmpty()) {
            return new Object[0][0];
        }
        // 3. 第二步：分批获取详细数据 (每 5 个日期一批)
        List<Map<String, Object>> allRows = new ArrayList<>();
        int batchSize = 5;

        // 用于记录表头，确保顺序一致
        LinkedHashMap<String, Integer> finalHeaders = new LinkedHashMap<>();
        int limitNum = Math.min(reportDates.size(), maxNum / 4);
        for (int i = 0; i < limitNum; i += batchSize) {
            int end = Math.min(i + batchSize, limitNum);
            List<String> batchDates = reportDates.subList(i, end);
            String datesParam = String.join(",", batchDates);

            UriComponentsBuilder dataBuilder = UriComponentsBuilder.fromHttpUrl(baseUrlData)
                    .queryParam("companyType", companyType)
                    .queryParam("reportDateType", "0")
                    .queryParam("reportType", "1") // 1: 利润表
                    .queryParam("code", symbol)
                    .queryParam("dates", datesParam);
            try {
                HttpEntity<Void> requestEntity = new HttpEntity<>(buildHeaders("emweb.securities.eastmoney.com"));
                ResponseEntity<String> response = restTemplate.exchange(dataBuilder.toUriString(),
                        HttpMethod.GET, requestEntity, String.class);
                if (response.getBody() != null) {
                    JSONObject root = JSONObject.parseObject(response.getBody());
                    if (root.containsKey("data")) {
                        JSONArray dataList = root.getJSONArray("data");
                        if (dataList != null && !dataList.isEmpty()) {
                            JSONObject firstItem = dataList.getJSONObject(0);
                            for (String s : firstItem.keySet()) {
                                if (!finalHeaders.containsKey(s)) {
                                    finalHeaders.put(s, 0);
                                }
                            }
                            for (int j = 0; j < dataList.size(); j++) {
                                JSONObject rowObj = dataList.getJSONObject(j);
                                Map<String, Object> rowMap = new HashMap<>();

                                for (Map.Entry<String, Integer> entry : finalHeaders.entrySet()) {
                                    String key = entry.getKey();
                                    Integer nullCount = entry.getValue() == null ? 0 : entry.getValue();
                                    // 处理空值或特定格式
                                    Object val = rowObj.get(key);
                                    // Python 代码中有 pd.to_numeric 处理，这里保持原样，由调用方处理或转为 String
                                    // 如果需要统一转为 String 以便构建 Object[][]:
                                    if (val == null ||
                                            (val instanceof String && !StringUtils.hasText((String) val))) {
                                        nullCount++;
                                        rowMap.put(key, "");
                                    } else {
                                        // 如果是数字，保留数字对象；如果是日期对象，转字符串
                                        rowMap.put(key, val.toString());
                                    }
                                    finalHeaders.put(key, nullCount);
                                }
                                allRows.add(rowMap);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch profit sheet data batch for dates: {}, symbol: {}", datesParam, symbol, e);
            }
            // 礼貌性延时，避免触发反爬 (虽然 RestTemplate 同步阻塞，但在高并发调用外部服务时建议控制)
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        // 4. 构建返回结果 Object[][]
        if (allRows.isEmpty()) {
            return new Object[0][0];
        }
        //全部为空的列去除
        List<String> columns = new ArrayList<>();
        finalHeaders.forEach((k, v) -> {
            if (!Objects.equals(allRows.size(), v)) {
                columns.add(k);
            }
        });
        // 优化：将 REPORT_DATE 放到第一列 (可选)
        if (columns.contains("REPORT_DATE")) {
            columns.remove("REPORT_DATE");
            columns.add(0, "REPORT_DATE");
        }
        int colCount = columns.size();
        int rowCount = allRows.size();
        Object[][] result = new Object[rowCount + 1][colCount];

        result[0] = columns.toArray();
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> rowMap = allRows.get(i);
            for (int j = 0; j < colCount; j++) {
                String key = columns.get(j);
                result[i + 1][j] = rowMap.get(key);
            }
        }
        return result;
    }

    /**
     *
     * @param symbol
     * @return
     */
    private String getCompanyType(String symbol) {
        return "4";
    }
}
