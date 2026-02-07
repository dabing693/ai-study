package com.lyh.finance.model.chat.property;

import lombok.Data;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Data
public class ChatModelProperty {
    protected String baseUrl;
    protected String apiKey;
    protected String modelName;
    protected Boolean enableThinking;
}
