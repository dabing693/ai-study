package com.lyh.finance.model.chat;

import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.FunctionTool;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.model.chat.config.ModelProperty;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
public abstract class ChatModel {
    protected ModelProperty modelProperty;
    protected RestTemplate restTemplate;

    public ChatModel(ModelProperty modelProperty,
                     RestTemplate restTemplate) {
        this.modelProperty = modelProperty;
        this.restTemplate = restTemplate;
    }

    public abstract ChatResponse call(List<Message> messages, List<FunctionTool> tools);
}
