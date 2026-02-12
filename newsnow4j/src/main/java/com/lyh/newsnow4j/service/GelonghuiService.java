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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GelonghuiService {

    @Autowired
    private NewsItemMapper newsItemMapper;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "https://www.gelonghui.com";

    public GelonghuiService() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler());
        this.objectMapper = new ObjectMapper();
    }

    public List<NewsItemDto> getNews() {
        try {
            String apiUrl = BASE_URL + "/news/";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Referer", "https://www.gelonghui.com/");

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

                var mainElement = doc.select(".article-content");
                if (mainElement.isEmpty()) {
                    log.warn("Could not find .article-content element");
                    return getCachedNews("gelonghui");
                }

                for (Element el : mainElement) {
                    var linkElement = el.select(".detail-right > a").first();
                    if (linkElement == null) {
                        continue;
                    }

                    String url = linkElement.attr("href");
                    if (url == null || url.isEmpty()) {
                        continue;
                    }

                    var titleElement = linkElement.select("h2").first();
                    String title = titleElement != null ? titleElement.text() : "";
                    if (title.isEmpty()) {
                        continue;
                    }

                    var timeElements = el.select(".time > span");
                    if (timeElements.size() < 3) {
                        continue;
                    }

                    String info = timeElements.get(0).text();
                    String relativeTime = timeElements.get(2).text();
                    if (relativeTime == null || relativeTime.isEmpty()) {
                        continue;
                    }

                    NewsItem newsItem = new NewsItem();
                    newsItem.setItemId(url);
                    newsItem.setTitle(title);
                    newsItem.setUrl(BASE_URL + url);
                    newsItem.setSource("gelonghui");
                    newsItem.setPubDate(parseRelativeDate(relativeTime));

                    Map<String, Object> extraInfo = Map.of("info", info);
                    newsItem.setExtraInfo(extraInfo.toString());

                    savedItems.add(newsItem);
                }

                if (!savedItems.isEmpty()) {
                    log.debug("Saving {} items to database", savedItems.size());
                    newsItemMapper.insert(savedItems);

                    return savedItems.stream()
                            .map(this::convertToNewsItemDto)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.error("Error fetching news from Gelonghui", e);
        }

        return getCachedNews("gelonghui");
    }

    private long parseRelativeDate(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
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

        Map<String, Object> extra = new java.util.HashMap<>();
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
