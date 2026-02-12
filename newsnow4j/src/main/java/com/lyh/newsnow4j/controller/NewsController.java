package com.lyh.newsnow4j.controller;

import com.lyh.newsnow4j.domain.dto.SourceResponse;
import com.lyh.newsnow4j.domain.dto.NewsItemDto;
import com.lyh.newsnow4j.service.ClsService;
import com.lyh.newsnow4j.service.MktNewsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
public class NewsController {
    @Value("${news.max.num:30}")
    private Integer maxNum;
    @Autowired
    private ClsService clsService;

    @Autowired
    private MktNewsService mktNewsService;

    @GetMapping("/api/s")
    public ResponseEntity<SourceResponse> getSource(@RequestParam("id") String id,
                                                    @RequestParam(required = false, defaultValue = "false") String latest) {
        try {
            boolean isLatest = !latest.equals("false");
            List<NewsItemDto> items = new ArrayList<>();
            switch (id) {
                case "cls":
                case "cls-telegraph":
                    items = clsService.getTelegraphNews();
                    break;
                case "cls-depth":
                    items = clsService.getDepthNews();
                    break;
                case "cls-hot":
                    items = clsService.getHotNews();
                    break;
                case "mktnews":
                case "mktnews-flash":
                    items = mktNewsService.getFlashNews();
                    break;
                default:
                    log.warn("未支持数据源: {}", id);
                    break;
            }
            if (items.size() > maxNum) {
                items = items.subList(0, maxNum);
            }
            SourceResponse response = new SourceResponse();
            response.setStatus("success");
            response.setId(id);
            response.setUpdatedTime(System.currentTimeMillis());
            response.setItems(items);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("处理请求失败，来源: {}", id, e);
            List<NewsItemDto> cachedItems = Collections.emptyList();
            SourceResponse response = new SourceResponse();
            response.setStatus("cache");
            response.setId(id);
            response.setUpdatedTime(System.currentTimeMillis());
            response.setItems(cachedItems);

            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/icons/{sourceId}.png")
    public ResponseEntity<Resource> getImage(@PathVariable String sourceId) {
        Resource resource = new ClassPathResource("icons/" + sourceId + ".png");
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}