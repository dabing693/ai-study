package com.lyh.trade.tools;


import com.lyh.common.util.MarkdownUtil;
import com.lyh.trade.domain.dto.SearchCodeDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/1/25
 */
@Service
public class FundService {
    @Autowired
    private SearchService searchService;
    @Autowired
    private RestTemplate restTemplate;
    private static final String URL = "https://np-tjxg-b.eastmoney.com/api/smart-tag/fund/v3/pw/search-code";
    private static final Set<String> NO_SHOW_KEYS =
            new HashSet<>(Arrays.asList("SERIAL", "FUND_CODE", "FUND_MARKET_SHORT_NAME", "FUND_MARKET_NUM_APP", "FUND_INNER_NAME_APP"));

    @Tool(description = "选出符合条件的基金")
    public String selectFund(@ToolParam(description = "选基金条件") String selectStockCondition) {
        return request(selectStockCondition);
    }

    @Tool(description = "查询指定基金的数据")
    public String queryFund(@ToolParam(description = "基金查询条件") String fundQuery) {
        String res = request(fundQuery);
        return StringUtils.hasLength(res) ? res : searchService.search(fundQuery, null);
    }

    private String request(String query) {
        Map<String, Object> params = new HashMap<>();
        params.put("pageSize", 50);
        params.put("pageNo", 1);
        params.put("fingerprint", "5ffab92129f06e2f7d00e9f2d3f7c904");
        params.put("matchWord", "");
        params.put("shareToGuba", false);
        params.put("timestamp", System.currentTimeMillis());
        params.put("requestId", "bGzXlHNKgKB17gIDYcztTGKClG3anOzx1769308441308");
        params.put("removedConditionIdList", new ArrayList<>());
        params.put("ownSelectAll", false);
        params.put("needCorrect", true);
        params.put("client", "web");
        params.put("product", "");
        params.put("needShowStockNum", false);
        params.put("biz", "web_ai_select_stocks");
        params.put("gids", new ArrayList<>());
        params.put("dxInfo", new ArrayList<>());
        params.put("keyWord", query);
        params.put("customDataNew", "[{\"type\":\"text\",\"value\":\"%s\",\"extra\":\"\"}]".formatted(query));
        params.put("xcId", "");
        ResponseEntity<SearchCodeDTO> responseEntity = restTemplate.postForEntity(URL, params, SearchCodeDTO.class);
        SearchCodeDTO.Data.Result result = Optional.ofNullable(responseEntity)
                .map(ResponseEntity::getBody)
                .map(SearchCodeDTO::getData)
                .map(SearchCodeDTO.Data::getResult)
                .orElse(null);
        if (result == null) {
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
