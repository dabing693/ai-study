package com.lyh.finance.tools;


import com.lyh.common.util.MarkdownUtil;
import com.lyh.finance.annotation.Tool;
import com.lyh.finance.annotation.ToolParam;
import com.lyh.finance.domain.dto.TavilySearchDTO;
import com.lyh.finance.domain.query.TavilySearchQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class SearchService {
    @Value("${tavily.api.key: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}")
    private String tavilyApiKey;
    @Autowired
    private RestTemplate restTemplate;
    private static final Object[] head = {"标题", "内容"};
    private static final String TAVILY_API_BASE = "https://api.tavily.com";
    private static final String TAVILY_API_SEARCH = TAVILY_API_BASE + "/search";

    @Tool(description = """
            搜索引擎，可以搜索一切你想了解的内容！比如股票的消息、新闻等数据。
            startDate只有当用户有日期要求时才填，否则不填。
            """)
    public String search(@ToolParam(description = "搜索引擎query") String query,
                         @ToolParam(description = "开始日期\n格式：yyyy-MM-dd", required = false) String startDate) {
        TavilySearchQuery request = TavilySearchQuery.builder()
                .query(query)
                .topic("general")
                .search_depth("basic")
                .chunks_perSource(3)
                .max_results(5)
                .include_answer(false)
                .include_raw_content(false)
                .include_images(false)
                .include_image_descriptions(false)
                .start_date(startDate)
                .build();

        TavilySearchDTO searchDTO = search(request);
        List<TavilySearchDTO.TavilySearchResult> searchResults = Optional.ofNullable(searchDTO).map(TavilySearchDTO::getResults).orElse(null);
        Object[][] array = new Object[searchResults.size() + 1][2];
        array[0] = head;
        for (int i = 0; i < searchResults.size(); i++) {
            TavilySearchDTO.TavilySearchResult res = searchResults.get(i);
            array[i + 1] = new Object[]{res.getTitle(), res.getContent()};
        }
        return MarkdownUtil.arrayToMarkdownTable(array);
    }

    private TavilySearchDTO search(TavilySearchQuery request) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Authorization", "Bearer " + tavilyApiKey);
        HttpEntity<TavilySearchQuery> httpEntity = new HttpEntity<>(request, requestHeaders);

        return restTemplate.postForObject(TAVILY_API_SEARCH, httpEntity, TavilySearchDTO.class);
    }
}