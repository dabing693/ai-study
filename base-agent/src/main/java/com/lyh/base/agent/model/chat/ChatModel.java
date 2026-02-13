package com.lyh.base.agent.model.chat;

import com.lyh.base.agent.domain.ChatResponse;
import com.lyh.base.agent.domain.FunctionTool;
import com.lyh.base.agent.domain.StreamChatResult;
import com.lyh.base.agent.domain.StreamEvent;
import com.lyh.base.agent.domain.message.Message;
import com.lyh.base.agent.model.chat.property.ChatModelProperty;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
public abstract class ChatModel {
    protected static final String PAYLOAD_PREFIX = "data:";
    protected static final String PAYLOAD_END = "[DONE]";
    protected ChatModelProperty chatModelProperty;
    protected RestTemplate restTemplate;

    public ChatModel(ChatModelProperty chatModelProperty,
                     RestTemplate restTemplate) {
        this.chatModelProperty = chatModelProperty;
        this.restTemplate = restTemplate;
    }

    public ChatResponse call(Message message) {
        return call(List.of(message), null);
    }

    public ChatResponse call(Message message, List<FunctionTool> tools) {
        return call(List.of(message), tools);
    }

    public ChatResponse call(List<Message> messages) {
        return call(messages, null);
    }

    public abstract ChatResponse call(List<Message> messages, List<FunctionTool> tools);

    public abstract StreamChatResult stream(List<Message> messages,
                                            List<FunctionTool> tools,
                                            Consumer<StreamEvent> eventConsumer);

    /**
     * 响应式流接口
     *
     * @param messages
     * @param tools
     * @return
     */
    public Flux<StreamEvent> streamFlux(List<Message> messages, List<FunctionTool> tools) {
        return Flux.create(sink -> {
            try {
                this.stream(messages, tools, sink::next);
                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }
}
