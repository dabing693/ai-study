package com.lyh.finance.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lyh.finance.memory.RamMemory;
import com.lyh.finance.context.RequestContext;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.domain.message.SystemMessage;
import com.lyh.finance.domain.message.UserMessage;
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

    /**
     * 以下是业务参数
     *
     * @param content
     * @return
     */
    /**
     * 最大的消息数
     */
    @JsonInclude
    private int maxMessageNum = Integer.MAX_VALUE;

    public static ChatRequest initMessage(String systemPrompt,
                                          List<Message> hisMessages,
                                          String query,
                                          int maxMessageNum) {
        ChatRequest request = new ChatRequest();
        //先设置最大消息数
        request.setMaxMessageNum(maxMessageNum);
        request.addMessage(new SystemMessage(systemPrompt));
        hisMessages.forEach(request::addMessage);
        request.addMessage(new UserMessage(query));
        return request;
    }

    public ChatRequest addTool(List<FunctionTool> tools) {
        this.tools.addAll(tools);
        return this;
    }

    public ChatRequest addMessage(Message message) {
        RamMemory.add(RequestContext.getSession(), message);
        this.messages.add(message);
        if (this.messages.size() > maxMessageNum) {
            //todo 避免新消息被删除，导致没写入数据库
            List<Message> subMessages = this.getMessages().subList(this.messages.size() - maxMessageNum + 1,
                    this.messages.size());
            //System Prompt放第一条
            subMessages.add(0, this.messages.get(0));
        }
        return this;
    }

    public ChatRequest model(String model) {
        this.setModel(model);
        return this;
    }

    public ChatRequest enableThinking(boolean enable) {
        if (enable) {
            this.getThinking().setType("enabled");
        } else {
            this.getThinking().setType("disabled");
        }
        return this;
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