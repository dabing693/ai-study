package com.lyh.base.agent.domain.message;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lyh.base.agent.domain.DO.LlmMemory;
import com.lyh.base.agent.enums.MessageType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Data
public class Message {
    public static final String KEY_VALUABLE_CONTENTS = "valuableContents";

    private String role;
    private String content;
    @JsonIgnore
    private LocalDateTime create = LocalDateTime.now();
    @JsonIgnore
    private boolean his;
    /**
     * 有价值的内容
     * tool执行结果用LLM提取或者代码层面拆分为多条利于搜索的内容，存入向量数据库
     */
    @JsonIgnore
    protected List<String> valuableContents;

    public Message() {

    }

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String storedContent() {
        return this.content;
    }


    public String jsonContent() {
        JSONObject jsonObject = JSONObject.from(this);
        if (valuableContents != null && !valuableContents.isEmpty()) {
            jsonObject.put(Message.KEY_VALUABLE_CONTENTS, this.valuableContents);
        }
        return jsonObject.toString();
    }

    public static Message fromMemory(LlmMemory it) {
        Message message = null;
        String jsonContent = it.getJsonContent();
        MessageType role = it.getType();
        if (Objects.equals(role, MessageType.system)) {
            message = JSONObject.parseObject(jsonContent, SystemMessage.class);
        } else if (Objects.equals(role, MessageType.user)) {
            message = JSONObject.parseObject(jsonContent, UserMessage.class);
        } else if (Objects.equals(role, MessageType.tool)) {
            message = JSONObject.parseObject(jsonContent, ToolMessage.class);
        } else if (Objects.equals(role, MessageType.assistant)) {
            message = JSONObject.parseObject(jsonContent, AssistantMessage.class);
        } else {
            throw new RuntimeException("未知消息类型：" + role);
        }
        message.setCreate(it.getTimestamp());
        message.setHis(true);
        return message;
    }
}