package com.lyh.base.agent.model.chat.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Data
@ConfigurationProperties(prefix = "model.zhipu")
public class ZhiPuChatModelProperty extends ChatModelProperty {
    public ZhiPuChatModelProperty() {
        this.baseUrl = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
        this.modelName = "glm-4.5-flash";
        this.enableThinking = true;
    }
}
