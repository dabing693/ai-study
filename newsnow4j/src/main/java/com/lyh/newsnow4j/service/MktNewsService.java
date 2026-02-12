package com.lyh.newsnow4j.service;

import com.lyh.newsnow4j.domain.dto.NewsItemDto;
import com.lyh.newsnow4j.domain.entity.NewsItem;
import com.lyh.newsnow4j.mapper.NewsItemMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MktNewsService {
    @Autowired
    private NewsItemMapper newsItemMapper;

    private final RestTemplate restTemplate;

    public MktNewsService() {
        this.restTemplate = new RestTemplate();
        // Configure the RestTemplate to handle errors gracefully
        this.restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler());
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<NewsItemDto> getFlashNews() {
        try {
            String apiUrl = "https://api.mktnews.net/api/flash?type=0&limit=50";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Referer", "https://mktnews.net/");
            headers.set("Origin", "https://mktnews.net");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Fetching data from: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.debug("API Response Status: {}", response.getStatusCode());
            log.debug("API Response Body: {}", response.getBody());

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode statusNode = rootNode.get("status");
            JsonNode dataNode = rootNode.get("data");

            log.debug("API Status: {}, Data: {}", statusNode != null ? statusNode.asInt() : "null", dataNode != null ? dataNode.size() : "null");

            if (statusNode != null && statusNode.asInt() == 200 && dataNode != null && dataNode.isArray()) {
                List<JsonNode> items = new ArrayList<>();
                for (JsonNode item : dataNode) {
                    items.add(item);
                }

                // Sort by time descending (as in the original TypeScript code)
                items.sort((a, b) -> {
                    String timeB = b.get("time").asText();
                    String timeA = a.get("time").asText();
                    return timeB.compareTo(timeA);
                });

                // Convert to NewsItem entities and save to DB
                List<NewsItem> savedItems = new ArrayList<>();
                for (JsonNode item : items) {
                    NewsItem newsItem = convertToNewsItem(item, "mktnews-flash");
                    savedItems.add(newsItem);
                }

                // Save to database
                log.debug("Saving {} items to database", savedItems.size());
                newsItemMapper.insert(savedItems);
                log.debug("Saved {} items to database", savedItems.size());

                // Convert to DTOs
                List<NewsItemDto> result = savedItems.stream()
                        .map(this::convertToNewsItemDto)
                        .collect(Collectors.toList());

                log.debug("Returning {} items", result.size());
                return result;
            } else {
                log.warn("API returned unexpected status or data format. Status: {}, Data: {}",
                        statusNode != null ? statusNode.asInt() : "null",
                        dataNode != null ? dataNode.size() : "null");
            }
        } catch (Exception e) {
            log.error("Error fetching flash news from MKTNews", e);
        }

        // Return cached data if available
        log.debug("Returning cached data for mktnews-flash");
        return getCachedNews("mktnews-flash");
    }

    private NewsItem convertToNewsItem(JsonNode item, String source) {
        NewsItem newsItem = new NewsItem();
        newsItem.setItemId(item.get("id").asText());

        // Extract title - use title if available, otherwise extract from content
        JsonNode dataNode = item.get("data");
        String title = null;
        if (dataNode.has("title") && !dataNode.get("title").isNull()) {
            title = dataNode.get("title").asText();
        } else {
            String content = dataNode.get("content").asText();
            // Try to extract title from content if it starts with 【...】 pattern
            if (content != null) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^【([^】]*)】(.*)$");
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    title = matcher.group(1);
                } else {
                    title = content;
                }
            }
        }

        newsItem.setTitle(title);
        newsItem.setContent(dataNode.get("content").asText(""));
        newsItem.setUrl("https://mktnews.net/flashDetail.html?id=" + item.get("id").asText());
        newsItem.setPubDate(parseTimeToMillis(item.get("time").asText()));
        newsItem.setSource(source);
        newsItem.setImportant(item.get("important").asInt());

        return newsItem;
    }

    private String getTitle(JsonNode item) {
        JsonNode data = item.get("data");
        String title = null;
        if (data.has("title") && !data.get("title").isNull()) {
            title = data.get("title").asText();
        } else {
            String content = data.get("content").asText();
            // Try to extract title from content if it starts with 【...】 pattern
            if (content != null && content.startsWith("【")) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^【([^】]*)】(.*)$");
                java.util.regex.Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    title = matcher.group(1);
                } else {
                    title = content;
                }
            } else {
                title = content;
            }
        }

        return title;
    }

    private Long parseTimeToMillis(String timeStr) {
        if (StringUtils.hasText(timeStr)) {
            try {
                return new Date(timeStr).getTime();
            } catch (Exception e) {
                log.warn("new Date(timeStr) 格式化日期异常: {}", timeStr, e);
                try {
                    Instant instant = Instant.parse(timeStr);
                    return instant.toEpochMilli();
                } catch (Exception ex) {
                    log.warn("Instant.parse(timeStr) 格式化日期异常: {}", timeStr, e);
                }
            }
        }
        return System.currentTimeMillis();
    }

    private NewsItemDto convertToNewsItemDto(NewsItem newsItem) {
        NewsItemDto dto = new NewsItemDto();
        dto.setId(newsItem.getItemId());
        dto.setTitle(newsItem.getTitle());
        dto.setUrl(newsItem.getUrl());
        dto.setPubDate(newsItem.getPubDate());

        // Create extra map with info and hover
        Map<String, Object> extra = new HashMap<>();
        if (newsItem.getImportant() != null && newsItem.getImportant() == 1) {
            extra.put("info", "Important");
        }
        if (newsItem.getContent() != null) {
            extra.put("hover", newsItem.getContent());
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
            return new ArrayList<>(); // Return empty list if database query fails
        }
    }
}