package com.lyh.base.agent.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lyh.base.agent.domain.message.Message;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author lengYinHui
 * @date 2026/2/2
 */
@Data
public class ChatRequest {
    private final Thinking thinking = new Thinking();
    @JsonProperty(value = "do_sample")
    private boolean doSample = true;
    @JsonProperty(value = "max_tokens")
    private int maxTokens = 1000;
    private List<Message> messages = new ArrayList<>();
    private String model = "glm-4-flash";
    @JsonProperty(value = "request_id")
    private String requestId = UUID.randomUUID().toString().replace("-", "");
    private final List<String> stop = new ArrayList<>();
    private boolean stream = false;
    private double temperature = 0.7;
    private List<FunctionTool> tools = new ArrayList<>();
    @JsonProperty(value = "top_p")
    private double topP = 0.9;
    private String user = "user";

    public void setEnableThinking(boolean enable) {
        if (enable) {
            this.getThinking().setType("enabled");
        } else {
            this.getThinking().setType("disabled");
        }
    }

    @Data
    public static class Thinking {
        /**
         * enabled
         * disabled
         */
        private String type = "enabled";
    }
}