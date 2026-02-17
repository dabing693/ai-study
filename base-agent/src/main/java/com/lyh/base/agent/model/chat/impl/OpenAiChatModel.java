package com.lyh.base.agent.model.chat.impl;

import com.lyh.base.agent.domain.ChatResponse;
import com.lyh.base.agent.domain.FunctionTool;
import com.lyh.base.agent.domain.StreamChatResult;
import com.lyh.base.agent.domain.StreamEvent;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.model.chat.ChatModel;
import com.lyh.base.agent.model.chat.property.ChatModelProperty;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.function.Consumer;

public class OpenAiChatModel extends ChatModel {
    public OpenAiChatModel(ChatModelProperty chatModelProperty, RestTemplate restTemplate) {
        super(chatModelProperty, restTemplate);
    }

    @Override
    public ChatResponse call(List<Message> messages, List<FunctionTool> tools) {
        return null;
    }

    @Override
    public StreamChatResult stream(List<Message> messages, List<FunctionTool> tools, Consumer<StreamEvent> eventConsumer) {
        return null;
    }
}
