package com.lyh.newsnow4j.domain.dto;

import lombok.Data;

import java.util.Map;

@Data
public class NewsItemDto {
    private String id;
    private String title;
    private String url;
    private String mobileUrl;
    private Long pubDate;
    private Map<String, Object> extra;
}