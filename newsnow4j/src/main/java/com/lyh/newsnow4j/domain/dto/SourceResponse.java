package com.lyh.newsnow4j.domain.dto;

import lombok.Data;
import java.util.List;

@Data
public class SourceResponse {
    private String status;
    private String id;
    private Long updatedTime;
    private List<NewsItemDto> items;
}