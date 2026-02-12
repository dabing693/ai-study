package com.lyh.newsnow4j.domain.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lyh.newsnow4j.domain.entity.NewsItem;
import lombok.Data;
import java.util.List;

@Data
public class ClsApiResponse {
    private DataInfo data;
    
    @Data
    public static class DataInfo {
        @JsonProperty("roll_data")
        private List<ClsItem> rollData;
        
        @JsonProperty("top_article")
        private List<ClsItem> topArticle;
        
        @JsonProperty("depth_list")
        private List<ClsItem> depthList;
    }
    
    @Data
    public static class ClsItem {
        private Integer id;
        private String title;
        private String brief;
        @JsonProperty("shareurl")
        private String shareUrl;
        @JsonProperty("ctime")
        private Long ctime;  // Unix timestamp (seconds)
        @JsonProperty("is_ad")
        private Integer isAd;  // 1 for ad, 0 for regular content
    }

    public static NewsItem convertToNewsItem(ClsApiResponse.ClsItem item, String source) {
        NewsItem newsItem = new NewsItem();
        newsItem.setItemId(item.getId().toString());
        newsItem.setTitle(item.getTitle() != null ? item.getTitle() : item.getBrief());
        newsItem.setContent(item.getBrief());
        newsItem.setUrl("https://www.cls.cn/detail/" + item.getId());
        newsItem.setMobileUrl(item.getShareUrl());
        newsItem.setPubDate(item.getCtime() != null ? item.getCtime() * 1000 : System.currentTimeMillis()); // Convert to milliseconds
        newsItem.setSource(source);
        newsItem.setImportant(0); // Default to non-important

        return newsItem;
    }
}