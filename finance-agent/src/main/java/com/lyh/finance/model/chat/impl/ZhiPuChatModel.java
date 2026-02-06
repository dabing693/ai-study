package com.lyh.finance.model.chat.impl;

import com.alibaba.fastjson2.JSONArray;
import com.lyh.finance.domain.ChatRequest;
import com.lyh.finance.domain.ChatResponse;
import com.lyh.finance.domain.FunctionTool;
import com.lyh.finance.domain.message.Message;
import com.lyh.finance.model.chat.ChatModel;
import com.lyh.finance.model.chat.config.ModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Slf4j
public class ZhiPuChatModel extends ChatModel {


    public ZhiPuChatModel(ModelProperty modelProperty,
                          RestTemplate restTemplate) {
        super(modelProperty, restTemplate);
    }

    @Override
    public ChatResponse call(List<Message> messages, List<FunctionTool> tools) {
        //构造对话请求
        ChatRequest request = new ChatRequest();
        request.setMessages(messages);
        request.setModel(modelProperty.getModelName());
        request.setEnableThinking(modelProperty.getEnableThinking());
        request.setTools(tools);
        //调用模型
        return call(request);
    }

    private ChatResponse call(ChatRequest request) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Authorization", "Bearer " + modelProperty.getApiKey());
        log.info("请求: {}", JSONArray.toJSONString(request));
        HttpEntity<ChatRequest> httpEntity = new HttpEntity<>(request, requestHeaders);
        return restTemplate.postForObject(modelProperty.getBaseUrl(), httpEntity, ChatResponse.class);
    }
}
