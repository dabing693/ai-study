package com.lyh.base.agent.domain.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@Data
public class Message {
    private String role;
    private String content;
    @JsonIgnore
    private LocalDateTime create = LocalDateTime.now();
    @JsonIgnore
    private boolean his;

    public Message() {

    }

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String storedContent() {
        return this.content;
    }
}