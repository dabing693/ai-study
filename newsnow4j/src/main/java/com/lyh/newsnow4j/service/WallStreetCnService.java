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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WallStreetCnService {

    @Autowired
    private NewsItemMapper newsItemMapper;

    private final RestTemplate restTemplate;

    public WallStreetCnService() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler());
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<NewsItemDto> getHotNews() {
        try {
            String apiUrl = "https://api-one.wallstcn.com/apiv1/content/articles/hot?period=all";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Referer", "https://wallstreetcn.com/");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Fetching hot news from: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.debug("API Response Status: {}", response.getStatusCode());

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode dayItemsNode = rootNode.path("data").path("day_items");

            if (dayItemsNode.isArray() && dayItemsNode.size() > 0) {
                List<NewsItem> savedItems = new ArrayList<>();
                for (JsonNode item : dayItemsNode) {
                    NewsItem newsItem = convertToNewsItem(item, "wallstreetcn-hot");
                    savedItems.add(newsItem);
                }

                log.debug("Saving {} items to database", savedItems.size());
                newsItemMapper.insert(savedItems);

                return savedItems.stream()
                        .map(this::convertToNewsItemDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching hot news from WallStreetCn", e);
        }

        return getCachedNews("wallstreetcn-hot");
    }

    @SuppressWarnings("unchecked")
    public List<NewsItemDto> getLatestNews() {
        try {
            String apiUrl = "https://api-one.wallstcn.com/apiv1/content/information_flow?channel=global-channel&accept=article&limit=30";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Referer", "https://wallstreetcn.com/");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Fetching latest news from: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
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
                    String resourceType = item.path("resource_type").asText("");
                    String type = item.path("resource").path("type").asText("");

                    if (!resourceType.equals("theme") && !resourceType.equals("ad") && 
                        !type.equals("live") && item.path("resource").has("uri")) {
                        
                        NewsItem newsItem = convertNewsToNewsItem(item.path("resource"), "wallstreetcn-news");
                        savedItems.add(newsItem);
                    }
                }

                log.debug("Saving {} items to database", savedItems.size());
                newsItemMapper.insert(savedItems);

                return savedItems.stream()
                        .map(this::convertToNewsItemDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching latest news from WallStreetCn", e);
        }

        return getCachedNews("wallstreetcn-news");
    }

    @SuppressWarnings("unchecked")
    public List<NewsItemDto> getLiveNews() {
        try {
            String apiUrl = "https://api-one.wallstcn.com/apiv1/content/lives?channel=global-channel&limit=30";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Referer", "https://wallstreetcn.com/");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Fetching live news from: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
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
                    NewsItem newsItem = convertToNewsItem(item, "wallstreetcn");
                    savedItems.add(newsItem);
                }

                log.debug("Saving {} items to database", savedItems.size());
                newsItemMapper.insert(savedItems);

                return savedItems.stream()
                        .map(this::convertToNewsItemDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching live news from WallStreetCn", e);
        }

        return getCachedNews("wallstreetcn");
    }

    private NewsItem convertToNewsItem(JsonNode item, String source) {
        NewsItem newsItem = new NewsItem();
        newsItem.setItemId(String.valueOf(item.path("id").asLong()));
        newsItem.setTitle(item.path("title").asText(item.path("content_text").asText("")));
        newsItem.setUrl(item.path("uri").asText(""));
        newsItem.setPubDate(item.path("display_time").asLong(0) * 1000);
        newsItem.setSource(source);
        newsItem.setContent(item.path("content_short").asText(""));
        return newsItem;
    }

    private NewsItem convertNewsToNewsItem(JsonNode item, String source) {
        NewsItem newsItem = new NewsItem();
        newsItem.setItemId(String.valueOf(item.path("id").asLong()));
        newsItem.setTitle(item.path("title").asText(item.path("content_short").asText("")));
        newsItem.setUrl(item.path("uri").asText(""));
        newsItem.setPubDate(item.path("display_time").asLong(0) * 1000);
        newsItem.setSource(source);
        newsItem.setContent(item.path("content_short").asText(""));
        return newsItem;
    }

    private NewsItemDto convertToNewsItemDto(NewsItem newsItem) {
        NewsItemDto dto = new NewsItemDto();
        dto.setId(newsItem.getItemId());
        dto.setTitle(newsItem.getTitle());
        dto.setUrl(newsItem.getUrl());
        dto.setPubDate(newsItem.getPubDate());
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
