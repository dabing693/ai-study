package com.lyh.newsnow4j.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyh.newsnow4j.domain.dto.NewsItemDto;
import com.lyh.newsnow4j.domain.entity.NewsItem;
import com.lyh.newsnow4j.mapper.NewsItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class XueqiuService {

    @Autowired
    private NewsItemMapper newsItemMapper;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public XueqiuService() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler());
        this.objectMapper = new ObjectMapper();
    }

    public List<NewsItemDto> getHotStock() {
        try {
            String url = "https://stock.xueqiu.com/v5/stock/hot_stock/list.json?size=30&_type=10&type=10";

            List<String> cookies = getCookies();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/json, */*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Referer", "https://xueqiu.com/hq");
            if (cookies != null && !cookies.isEmpty()) {
                headers.set("Cookie", String.join("; ", cookies));
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Fetching hot stock from: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.debug("API Response Status: {}", response.getStatusCode());

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode itemsNode = rootNode.path("data").path("items");

            if (itemsNode.isArray() && itemsNode.size() > 0) {
                List<NewsItem> savedItems = new ArrayList<>();
                for (JsonNode item : itemsNode) {
                    if (item.path("ad").asInt() == 1) {
                        continue;
                    }

                    NewsItem newsItem = new NewsItem();
                    newsItem.setItemId(item.path("code").asText());
                    newsItem.setTitle(item.path("name").asText());
                    newsItem.setUrl("https://xueqiu.com/s/" + item.path("code").asText());
                    newsItem.setSource("xueqiu-hotstock");
                    newsItem.setPubDate(System.currentTimeMillis());

                    Map<String, Object> extraInfo = new HashMap<>();
                    extraInfo.put("info", item.path("percent").asText() + "% " + item.path("exchange").asText());
                    newsItem.setExtraInfo(extraInfo.toString());

                    savedItems.add(newsItem);
                }

                log.debug("Saving {} items to database", savedItems.size());
                newsItemMapper.insert(savedItems);

                return savedItems.stream()
                        .map(this::convertToNewsItemDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching hot stock from Xueqiu", e);
        }

        return getCachedNews("xueqiu-hotstock");
    }

    private List<String> getCookies() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "https://xueqiu.com/hq",
                    String.class
            );

            List<String> cookies = response.getHeaders().get("Set-Cookie");
            return cookies != null ? cookies : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Failed to get cookies from Xueqiu", e);
            return new ArrayList<>();
        }
    }

    private NewsItemDto convertToNewsItemDto(NewsItem newsItem) {
        NewsItemDto dto = new NewsItemDto();
        dto.setId(newsItem.getItemId());
        dto.setTitle(newsItem.getTitle());
        dto.setUrl(newsItem.getUrl());
        dto.setPubDate(newsItem.getPubDate());

        Map<String, Object> extra = new HashMap<>();
        if (newsItem.getExtraInfo() != null) {
            try {
                Map<String, Object> infoMap = objectMapper.readValue(newsItem.getExtraInfo(), Map.class);
                extra.putAll(infoMap);
            } catch (Exception e) {
                log.warn("Failed to parse extra info", e);
            }
        }
        dto.setExtra(extra);

        return dto;
    }

    private List<NewsItemDto> getCachedNews(String source) {
        try {
            List<NewsItem> cachedItems = newsItemMapper.findLatestBySource(source, 30);
            log.debug("Retrieved {} cached items for source: {}", cachedItems.size(), source);
            return cachedItems.stream()
                    .map(this::convertToNewsItemDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving cached news for source: {}", source, e);
            return new ArrayList<>();
        }
    }
}
