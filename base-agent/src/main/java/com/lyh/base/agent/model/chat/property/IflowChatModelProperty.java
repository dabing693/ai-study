package com.lyh.base.agent.model.chat.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Iflow模型配置
 */
@Data
@ConfigurationProperties(prefix = "model.iflow")
public class IflowChatModelProperty extends ChatModelProperty {
    public IflowChatModelProperty() {
        this.baseUrl = "https://apis.iflow.cn/v1/chat/completions";
        this.modelName = "qwen3-max";
        this.enableThinking = false;
    }
}
