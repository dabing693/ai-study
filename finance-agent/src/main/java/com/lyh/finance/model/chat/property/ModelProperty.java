package com.lyh.finance.model.chat.property;

import lombok.Data;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Data
public class ModelProperty {
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private String apiKey;
    private String modelName = "glm-4.5-flash";
    private Boolean enableThinking = true;
}
