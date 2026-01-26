package com.lyh.trade.domain.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TavilySearchDTO {
    private String query;
    private String answer;
    private List<String> images;
    private List<TavilySearchResult> results;
    private String response_time;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TavilySearchResult {
        private String title;
        private String url;
        private String content;
        private Double score;
        private String raw_content;
    }
}
