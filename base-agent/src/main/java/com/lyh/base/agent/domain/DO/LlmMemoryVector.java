package com.lyh.base.agent.domain.DO;

import lombok.Data;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/2/5
 */
@Data
public class LlmMemoryVector {
    private Long id;
    private String type;
    private String conversation_id;
    private String content;
    private List<Float> content_vector;
}
