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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class Jin10Service {

    @Autowired
    private NewsItemMapper newsItemMapper;

    private final RestTemplate restTemplate;

    public Jin10Service() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler());
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern TITLE_PATTERN = Pattern.compile("^【([^】]*)】(.*)$");

    public List<NewsItemDto> getFlashNews() {
        try {
            long timestamp = System.currentTimeMillis();
            String apiUrl = "https://www.jin10.com/flash_newest.js?t=" + timestamp;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/javascript, */*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Referer", "https://www.jin10.com/");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Fetching flash news from: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.debug("API Response Status: {}", response.getStatusCode());

            String rawData = response.getBody();
            if (rawData != null) {
                String jsonStr = rawData
                        .replaceFirst("^var\\s+newest\\s*=\\s*", "")
                        .replaceAll(";*$", "")
                        .trim();

                JsonNode itemsNode = objectMapper.readTree(jsonStr);
                
                if (itemsNode.isArray() && itemsNode.size() > 0) {
                    List<NewsItem> savedItems = new ArrayList<>();
                    for (JsonNode item : itemsNode) {
                        JsonNode dataNode = item.path("data");
                        String title = dataNode.path("title").asText("");
                        String content = dataNode.path("content").asText("");

                        if ((title.isEmpty() && content.isEmpty()) || 
                            item.has("channel") && item.path("channel").asInt() == 5) {
                            continue;
                        }

                        String text = (title.isEmpty() ? content : title).replaceAll("</?b>", "");
                        Matcher matcher = TITLE_PATTERN.matcher(text);
                        String extractedTitle = null;
                        String desc = null;

                        if (matcher.find()) {
                            extractedTitle = matcher.group(1);
                            desc = matcher.group(2);
                        }

                        NewsItem newsItem = new NewsItem();
                        newsItem.setItemId(item.path("id").asText());
                        newsItem.setTitle(extractedTitle != null ? extractedTitle : text);
                        newsItem.setUrl("https://flash.jin10.com/detail/" + item.path("id").asText());
                        newsItem.setPubDate(parseRelativeDate(item.path("time").asText("")));
                        newsItem.setSource("jin10");
                        newsItem.setImportant(item.path("important").asInt());

                        Map<String, Object> extraInfo = new HashMap<>();
                        if (desc != null) {
                            extraInfo.put("hover", desc);
                        }
                        if (item.path("important").asInt() == 1) {
                            extraInfo.put("info", "✰");
                        }
                        newsItem.setExtraInfo(extraInfo.isEmpty() ? null : extraInfo.toString());

                        savedItems.add(newsItem);
                    }

                    log.debug("Saving {} items to database", savedItems.size());
                    newsItemMapper.insert(savedItems);

                    return savedItems.stream()
                            .map(this::convertToNewsItemDto)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.error("Error fetching flash news from Jin10", e);
        }

        return getCachedNews("jin10");
    }

    private long parseRelativeDate(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            java.util.Date date = sdf.parse(timeStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", timeStr, e);
            return System.currentTimeMillis();
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
