package com.lyh.finance.model;

import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.config.ModelProperty;
import com.lyh.finance.tool.ToolBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
public abstract class ChatModel {
    protected ModelProperty modelProperty;
    protected RestTemplate restTemplate;
    protected MemoryManager memoryManager;
    protected ToolBuilder toolBuilder;

    public ChatModel(ModelProperty modelProperty,
                     RestTemplate restTemplate,
                     MemoryManager memoryManager,
                     ToolBuilder toolBuilder
    ) {
        this.modelProperty = modelProperty;
        this.restTemplate = restTemplate;
        this.memoryManager = memoryManager;
        this.toolBuilder = toolBuilder;
    }

    public ChatResponse call(Message message) {
        return call(List.of(message));
    }

    public abstract ChatResponse call(List<Message> messages);
}
