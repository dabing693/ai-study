package com.lyh.newsnow4j.domain.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lyh.newsnow4j.domain.entity.NewsItem;
import lombok.Data;

import java.util.List;

@Data
public class ClsHotListApiResponse {
    private List<ClsItem> data;

    @Data
    public static class ClsItem {
        private Integer id;
        private String title;
        private String brief;
        @JsonProperty("shareurl")
        private String shareUrl;
        @JsonProperty("ctime")
        private Long ctime;
        @JsonProperty("is_ad")
        private Integer isAd;
    }

    public static NewsItem convertToNewsItem(ClsHotListApiResponse.ClsItem item, String source) {
        NewsItem newsItem = new NewsItem();
        newsItem.setItemId(item.getId().toString());
        newsItem.setTitle(item.getTitle() != null ? item.getTitle() : item.getBrief());
        newsItem.setContent(item.getBrief());
        newsItem.setUrl("https://www.cls.cn/detail/" + item.getId());
        newsItem.setMobileUrl(item.getShareUrl());
        newsItem.setPubDate(item.getCtime() != null ? item.getCtime() * 1000 : System.currentTimeMillis()); // Convert to milliseconds
        newsItem.setSource(source);
        newsItem.setImportant(0);
        return newsItem;
    }
}
