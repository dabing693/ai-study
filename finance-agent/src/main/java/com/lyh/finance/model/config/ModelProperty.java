package com.lyh.finance.model.config;

import lombok.Data;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Data
public class ModelProperty {
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Boolean enableThinking;
}
