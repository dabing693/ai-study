package com.lyh.finance.tools;

import com.lyh.common.util.MarkdownUtil;
import com.lyh.finance.annotation.Tool;
import com.lyh.finance.annotation.ToolParam;
import com.lyh.finance.domain.dto.SearchCodeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/1/25
 */
@Service
public class StockService {
    @Autowired
    private SearchService searchService;
    @Autowired
    private RestTemplate restTemplate;
    private static final String URL = "https://np-tjxg-b.eastmoney.com/api/smart-tag/stock/v3/pw/search-code";
    private static final Set<String> NO_SHOW_KEYS =
            new HashSet<>(Arrays.asList("SERIAL", "SECURITY_CODE", "MARKET_SHORT_NAME", "MARKET_NUM"));

    @Tool(description = "选出符合条件的股票")
    public String selectStock(@ToolParam(description = "选股条件") String selectStockCondition) {
        return request(selectStockCondition);
    }

    @Tool(description = """
            查询指定股票的数据，比如其成交量、涨跌幅等行情数据，营业收入、利润等财务数据；不包含股票的市场新闻和公告等数据！
            例句1：腾讯的成立时间
            例句2：腾讯的成交量、市值、换手率
            """)
    public String queryStock(@ToolParam(description = "股票查询条件") String stockQuery) {
        String res = request(stockQuery);
        return StringUtils.hasLength(res) ? res : searchService.search(stockQuery, null);
    }

    private String request(String query) {
        Map<String, Object> params = new HashMap<>();
        params.put("pageSize", 10);
        params.put("pageNo", 1);
        params.put("fingerprint", "5ffab92129f06e2f7d00e9f2d3f7c904");
        params.put("matchWord", "");
        params.put("shareToGuba", false);
        params.put("timestamp", System.currentTimeMillis());
        params.put("requestId", "bGzXlHNKgKB17gIDYcztTGKClG3anOzx1769308441308");
        params.put("removedConditionIdList", new ArrayList<>());
        params.put("ownSelectAll", false);
        params.put("needCorrect", true);
        params.put("client", "WEB");
        params.put("product", "");
        params.put("needShowStockNum", false);
        params.put("biz", "web_ai_select_stocks");
        params.put("gids", new ArrayList<>());
        params.put("dxInfoNew", new ArrayList<>());
        params.put("keyWordNew", query);
        params.put("customDataNew", "[{\"type\":\"text\",\"value\":\"%s\",\"extra\":\"\"}]".formatted(query));

        ResponseEntity<SearchCodeDTO> responseEntity = restTemplate.postForEntity(URL, params, SearchCodeDTO.class);
        SearchCodeDTO.Data.Result result = Optional.ofNullable(responseEntity)
                .map(ResponseEntity::getBody)
                .map(SearchCodeDTO::getData)
                .map(SearchCodeDTO.Data::getResult)
                .orElse(null);
        if (result == null || CollectionUtils.isEmpty(result.getDataList())) {
            return "";
        }
        List<SearchCodeDTO.Data.Result.Column> columns = result.getColumns()
                .stream().filter(it -> !NO_SHOW_KEYS.contains(it.getKey())).collect(Collectors.toList());
        List<Map<String, Object>> dataList = result.getDataList();
        Object[][] array = new Object[dataList.size() + 1][columns.size()];
        array[0] = columns.stream().map(SearchCodeDTO.Data.Result.Column::getTitle).toArray(String[]::new);
        for (int i = 0; i < dataList.size(); i++) {
            Object[] line = new Object[columns.size()];
            Map<String, Object> dataMap = dataList.get(i);
            for (int j = 0; j < columns.size(); j++) {
                line[j] = dataMap.get(columns.get(j).getKey());
            }
            array[i + 1] = line;
        }
        return MarkdownUtil.arrayToMarkdownTable(array);
    }
}
