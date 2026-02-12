package com.lyh.newsnow4j.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyh.newsnow4j.domain.dto.NewsItemDto;
import com.lyh.newsnow4j.domain.entity.NewsItem;
import com.lyh.newsnow4j.mapper.NewsItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FastbullService {

    @Autowired
    private NewsItemMapper newsItemMapper;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "https://www.fastbull.com";
    private static final Pattern TITLE_PATTERN = Pattern.compile("【(.+)】");

    public FastbullService() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler());
        this.objectMapper = new ObjectMapper();
    }

    public List<NewsItemDto> getExpressNews() {
        try {
            String apiUrl = BASE_URL + "/cn/express-news";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Referer", "https://www.fastbull.com/");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Fetching express news from: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.debug("API Response Status: {}", response.getStatusCode());

            String html = response.getBody();
            if (html != null) {
                Document doc = Jsoup.parse(html);
                List<NewsItem> savedItems = new ArrayList<>();

                var mainElement = doc.select(".news-list");
                if (mainElement.isEmpty()) {
                    log.warn("Could not find .news-list element");
                    return getCachedNews("fastbull-express");
                }

                for (Element el : mainElement) {
                    var linkElement = el.select(".title_name").first();
                    if (linkElement == null) {
                        continue;
                    }

                    String url = linkElement.attr("href");
                    if (url == null || url.isEmpty()) {
                        continue;
                    }

                    String titleText = linkElement.text();
                    if (titleText == null || titleText.isEmpty()) {
                        continue;
                    }

                    String title = titleText;
                    var matcher = TITLE_PATTERN.matcher(titleText);
                    if (matcher.find()) {
                        title = matcher.group(1);
                    }

                    String dateStr = el.attr("data-date");
                    if (dateStr == null || dateStr.isEmpty()) {
                        continue;
                    }

                    long date;
                    try {
                        date = Long.parseLong(dateStr);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse date: {}", dateStr);
                        continue;
                    }

                    NewsItem newsItem = new NewsItem();
                    newsItem.setItemId(url);
                    newsItem.setTitle(title.length() < 4 ? titleText : title);
                    newsItem.setUrl(BASE_URL + url);
                    newsItem.setSource("fastbull-express");
                    newsItem.setPubDate(date);

                    savedItems.add(newsItem);
                }

                if (!savedItems.isEmpty()) {
                    log.debug("Saving {} items to database", savedItems.size());
                    saveToDatabase(savedItems);

                    return savedItems.stream()
                            .map(this::convertToNewsItemDto)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.error("Error fetching express news from Fastbull", e);
        }

        return getCachedNews("fastbull-express");
    }

    public List<NewsItemDto> getNews() {
        try {
            String apiUrl = BASE_URL + "/cn/news";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Referer", "https://www.fastbull.com/");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Fetching news from: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.debug("API Response Status: {}", response.getStatusCode());

            String html = response.getBody();
            if (html != null) {
                Document doc = Jsoup.parse(html);
                List<NewsItem> savedItems = new ArrayList<>();

                var mainElement = doc.select(".trending_type");
                if (mainElement.isEmpty()) {
                    log.warn("Could not find .trending_type element");
                    return getCachedNews("fastbull-news");
                }

                for (Element el : mainElement) {
                    String url = el.attr("href");
                    if (url == null || url.isEmpty()) {
                        continue;
                    }

                    var titleElement = el.select(".title").first();
                    String title = titleElement != null ? titleElement.text() : "";
                    if (title == null || title.isEmpty()) {
                        continue;
                    }

                    var dateElement = el.select("[data-date]").first();
                    String dateStr = dateElement != null ? dateElement.attr("data-date") : "";
                    if (dateStr == null || dateStr.isEmpty()) {
                        continue;
                    }

                    long date;
                    try {
                        date = Long.parseLong(dateStr);
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse date: {}", dateStr);
                        continue;
                    }

                    NewsItem newsItem = new NewsItem();
                    newsItem.setItemId(url);
                    newsItem.setTitle(title);
                    newsItem.setUrl(BASE_URL + url);
                    newsItem.setSource("fastbull-news");
                    newsItem.setPubDate(date);

                    savedItems.add(newsItem);
                }

                if (!savedItems.isEmpty()) {
                    log.debug("Saving {} items to database", savedItems.size());
                    saveToDatabase(savedItems);

                    return savedItems.stream()
                            .map(this::convertToNewsItemDto)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.error("Error fetching news from Fastbull", e);
        }

        return getCachedNews("fastbull-news");
    }

    private void saveToDatabase(List<NewsItem> items) {
        for (NewsItem item : items) {
            com.lyh.newsnow4j.domain.entity.NewsItem newsItem = new com.lyh.newsnow4j.domain.entity.NewsItem();
            newsItem.setItemId(item.getItemId());
            newsItem.setTitle(item.getTitle());
            newsItem.setUrl(item.getUrl());
            newsItem.setPubDate(item.getPubDate());
            newsItem.setSource(item.getSource());
            newsItemMapper.insert(newsItem);
        }
    }

    private NewsItemDto convertToNewsItemDtoFromEntity(com.lyh.newsnow4j.domain.entity.NewsItem newsItem) {
        NewsItemDto dto = new NewsItemDto();
        dto.setId(newsItem.getItemId());
        dto.setTitle(newsItem.getTitle());
        dto.setUrl(newsItem.getUrl());
        dto.setPubDate(newsItem.getPubDate());
        return dto;
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
            List<com.lyh.newsnow4j.domain.entity.NewsItem> cachedItems = newsItemMapper.findLatestBySource(source, 30);
            log.debug("Retrieved {} cached items for source: {}", cachedItems.size(), source);
            return cachedItems.stream()
                    .map(this::convertToNewsItemDtoFromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error retrieving cached news for source: {}", source, e);
            return new ArrayList<>();
        }
    }
}
