package com.lyh.trade.self_react.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lyh.trade.self_react.domain.message.Message;
import com.lyh.trade.self_react.domain.message.UserMessage;
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
    @JsonProperty(value = "do_sample")
    private boolean doSample = true;
    @JsonProperty(value = "max_tokens")
    private int maxTokens = 1000;
    private final List<Message> messages = new ArrayList<>();
    private String model = "glm-4-flash";
    @JsonProperty(value = "request_id")
    private String requestId = UUID.randomUUID().toString().replace("-", "");
    private final List<String> stop = new ArrayList<>();
    private boolean stream = false;
    private double temperature = 0.7;
    private final List<FunctionTool> tools = new ArrayList<>();
    @JsonProperty(value = "top_p")
    private double topP = 0.9;
    private String user = "user";

    public static ChatRequest userMessage(String content) {
        ChatRequest request = new ChatRequest();
        UserMessage message = new UserMessage(content);
        request.getMessages().add(message);
        return request;
    }

    public ChatRequest addTool(List<FunctionTool> tools) {
        this.tools.addAll(tools);
        return this;
    }

    public ChatRequest addMessage(Message message) {
        this.messages.add(message);
        return this;
    }
}