package com.lyh.finance.model.impl;

import com.alibaba.fastjson2.JSONArray;
import com.lyh.finance.domain.ChatRequest;
import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.domain.message.SystemMessage;
import com.lyh.finance.domain.message.UserMessage;
import com.lyh.finance.memory.MemoryManager;
import com.lyh.finance.model.ChatModel;
import com.lyh.finance.model.config.ModelProperty;
import com.lyh.finance.tool.ToolBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Slf4j
public class ZhiPuChatModel extends ChatModel {
    private static final String system_prompt = """
            你是一个乐于助人的金融领域专家，你善于利用各种工具解决用户的问题。
            当前时间：{cur_date_time}
            """;

    public ZhiPuChatModel(ModelProperty modelProperty,
                          RestTemplate restTemplate,
                          MemoryManager memoryManager,
                          ToolBuilder toolBuilder
    ) {
        super(modelProperty, restTemplate, memoryManager, toolBuilder);
    }

    @Override
    public ChatResponse call(List<Message> messages) {
        //加载记忆
        List<Message> totalMessage = loadMemory(messages);
        //构造对话请求
        ChatRequest request = new ChatRequest();
        request.setMessages(totalMessage);
        request.setModel(modelProperty.getModelName());
        request.setEnableThinking(modelProperty.getEnableThinking());
        request.setTools(toolBuilder.getTools());
        //发往agent
        ChatResponse response = call(request);
        //保存记忆
        saveMemory(request, response);
        return response;
    }

    private List<Message> loadMemory(List<Message> messages) {
        //历史消息
        List<Message> hisMessages = new ArrayList<>();
        final List<Message> userMessages = messages.stream()
                .filter(it -> it instanceof UserMessage).collect(Collectors.toList());
        //用户消息不为空，才获取历史消息；tool、assistant消息意味着还在会话中
        if (!CollectionUtils.isEmpty(userMessages)) {
            String query = userMessages.get(userMessages.size() - 1).getContent();
            hisMessages = memoryManager.relatedHistoryMemory(query);
        }
        int maxHisMsg = memoryManager.getMaxMessageNum() - (1 + userMessages.size());
        if (hisMessages.size() > maxHisMsg) {
            //todo 避免新消息被删除，导致没写入数据库
            hisMessages = hisMessages.subList(hisMessages.size() - maxHisMsg, hisMessages.size());
        }
        List<Message> totalMessage = new ArrayList<>();
        //系统提示词
        totalMessage.add(systemPrompt());
        //历史消息
        totalMessage.addAll(hisMessages);
        //用户消息
        totalMessage.addAll(userMessages);
        return totalMessage;
    }

    private ChatResponse call(ChatRequest request) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Authorization", "Bearer " + modelProperty.getApiKey());
        log.info("请求: {}", JSONArray.toJSONString(request));
        HttpEntity<ChatRequest> httpEntity = new HttpEntity<>(request, requestHeaders);
        return restTemplate.postForObject(modelProperty.getBaseUrl(), httpEntity, ChatResponse.class);
    }

    private void saveMemory(ChatRequest request, ChatResponse response) {
        List<Message> totalMsg = request.getMessages().stream()
                //非历史消息才保存
                .filter(it -> !it.isHis())
                .collect(Collectors.toList());
        totalMsg.add(response.getMessage());
        memoryManager.save(totalMsg);
    }

    private SystemMessage systemPrompt() {
        return new SystemMessage(system_prompt.replace("{cur_date_time}",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS").format(new Date())));
    }
}
