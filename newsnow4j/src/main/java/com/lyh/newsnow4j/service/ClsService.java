package com.lyh.newsnow4j.service;

import com.lyh.newsnow4j.domain.dto.NewsItemDto;
import com.lyh.newsnow4j.domain.entity.NewsItem;
import com.lyh.newsnow4j.domain.response.ClsApiResponse;
import com.lyh.newsnow4j.domain.response.ClsHotListApiResponse;
import com.lyh.newsnow4j.mapper.NewsItemMapper;
import com.lyh.newsnow4j.utils.CryptoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ClsService {

    @Autowired
    private NewsItemMapper newsItemMapper;

    private final RestTemplate restTemplate;

    public ClsService() {
        this.restTemplate = new RestTemplate();
        // Configure the RestTemplate to handle errors gracefully
        this.restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler());
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Parameters required for the API call
    private final Map<String, String> baseParams = Map.of(
            "app", "CailianpressWeb",
            "os", "web",
            "sv", "7.7.5"
    );

    public List<NewsItemDto> getTelegraphNews() {
        try {
            String apiUrl = "https://www.cls.cn/nodeapi/updateTelegraphList";
            Map<String, String> params = new HashMap<>(baseParams);

            // Add sign parameter
            String paramString = buildParamString(params);
            String sign = CryptoUtils.generateSign(paramString);
            params.put("sign", sign);

            String fullUrl = apiUrl + "?" + buildQueryString(params);
            log.debug("Fetching data from: {}", fullUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            headers.set("Referer", "https://www.cls.cn/");
            headers.set("Origin", "https://www.cls.cn");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<ClsApiResponse> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    ClsApiResponse.class
            );

            log.debug("API Response Status: {}", response.getStatusCodeValue());

            ClsApiResponse apiResponse = response.getBody();
            if (apiResponse != null && apiResponse.getData() != null && apiResponse.getData().getRollData() != null) {
                List<ClsApiResponse.ClsItem> items = apiResponse.getData().getRollData().stream()
                        .filter(item -> item.getIsAd() == null || item.getIsAd() != 1) // Filter out ads
                        .collect(Collectors.toList());

                log.debug("Fetched {} items from API, filtered to {} items (removed ads)",
                        apiResponse.getData().getRollData().size(), items.size());

                // Convert to NewsItem entities and save to DB
                List<NewsItem> savedItems = new ArrayList<>();
                for (ClsApiResponse.ClsItem item : items) {
                    NewsItem newsItem = ClsApiResponse.convertToNewsItem(item, "cls-telegraph");
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
                log.warn("API returned unexpected response structure");
            }
        } catch (Exception e) {
            log.error("Error fetching telegraph news from Cailianpress", e);
        }

        // Return cached data if available
        log.debug("Returning cached data for cls-telegraph");
        return getCachedNews("cls-telegraph");
    }

    public List<NewsItemDto> getDepthNews() {
        try {
            String apiUrl = "https://www.cls.cn/v3/depth/home/assembled/1000";
            Map<String, String> params = new HashMap<>(baseParams);

            // Add sign parameter
            String paramString = buildParamString(params);
            String sign = CryptoUtils.generateSign(paramString);
            params.put("sign", sign);

            ResponseEntity<ClsApiResponse> response = restTemplate.getForEntity(
                    apiUrl + "?" + buildQueryString(params),
                    ClsApiResponse.class
            );

            ClsApiResponse apiResponse = response.getBody();
            if (apiResponse != null && apiResponse.getData() != null) {
                List<ClsApiResponse.ClsItem> allItems = new ArrayList<>();

                if (apiResponse.getData().getTopArticle() != null) {
                    allItems.addAll(apiResponse.getData().getTopArticle());
                }
                if (apiResponse.getData().getDepthList() != null) {
                    allItems.addAll(apiResponse.getData().getDepthList());
                }

                // Sort by time descending
                allItems.sort((a, b) -> Long.compare(b.getCtime(), a.getCtime()));

                // Convert to NewsItem entities and save to DB
                List<NewsItem> savedItems = new ArrayList<>();
                for (ClsApiResponse.ClsItem item : allItems) {
                    NewsItem newsItem = ClsApiResponse.convertToNewsItem(item, "cls-depth");
                    savedItems.add(newsItem);
                }

                // Save to database
                newsItemMapper.insert(savedItems);

                // Convert to DTOs
                return savedItems.stream()
                        .map(this::convertToNewsItemDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching depth news from Cailianpress", e);
        }

        // Return cached data if available
        return getCachedNews("cls-depth");
    }

    public List<NewsItemDto> getHotNews() {
        try {
            String apiUrl = "https://www.cls.cn/v2/article/hot/list";
            Map<String, String> params = new HashMap<>(baseParams);

            // Add sign parameter
            String paramString = buildParamString(params);
            String sign = CryptoUtils.generateSign(paramString);
            params.put("sign", sign);

            ResponseEntity<ClsHotListApiResponse> response = restTemplate.getForEntity(
                    apiUrl + "?" + buildQueryString(params),
                    ClsHotListApiResponse.class
            );

            ClsHotListApiResponse apiResponse = response.getBody();
            if (apiResponse != null && apiResponse.getData() != null && apiResponse.getData() != null) {
                List<ClsHotListApiResponse.ClsItem> items = apiResponse.getData();

                // Convert to NewsItem entities and save to DB
                List<NewsItem> savedItems = new ArrayList<>();
                for (ClsHotListApiResponse.ClsItem item : items) {
                    NewsItem newsItem = ClsHotListApiResponse.convertToNewsItem(item, "cls-hot");
                    savedItems.add(newsItem);
                }

                // Save to database
                newsItemMapper.insert(savedItems);

                // Convert to DTOs
                return savedItems.stream()
                        .map(this::convertToNewsItemDto)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Error fetching hot news from Cailianpress", e);
        }

        // Return cached data if available
        return getCachedNews("cls-hot");
    }


    private NewsItemDto convertToNewsItemDto(NewsItem newsItem) {
        NewsItemDto dto = new NewsItemDto();
        dto.setId(newsItem.getItemId());
        dto.setTitle(newsItem.getTitle());
        dto.setUrl(newsItem.getUrl());
        dto.setMobileUrl(newsItem.getMobileUrl());
        dto.setPubDate(newsItem.getPubDate());

        // Create extra map with info and hover
        Map<String, Object> extra = new HashMap<>();
        if (newsItem.getExtraInfo() != null) {
            extra.put("info", newsItem.getExtraInfo());
        }
        if (newsItem.getContent() != null) {
            extra.put("hover", newsItem.getContent());
        }
        if (newsItem.getImportant() != null && newsItem.getImportant() == 1) {
            extra.put("info", "Important");
        }

        dto.setExtra(extra);

        return dto;
    }

    private String buildParamString(Map<String, String> params) {
        // Sort parameters by key and build string
        List<String> paramPairs = new ArrayList<>();
        params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> paramPairs.add(entry.getKey() + "=" + entry.getValue()));

        return String.join("&", paramPairs);
    }

    private String buildQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
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