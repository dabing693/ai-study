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
    private Long msg_id;

    public static LlmMemoryVector of(LlmMemory it, String content, List<Float> content_vector) {
        LlmMemoryVector vector = new LlmMemoryVector();
        vector.setMsg_id(it.getId());
        vector.setType(it.getType().name());
        vector.setConversation_id(it.getConversationId());
        vector.setContent(content);
        vector.setContent_vector(content_vector);
        return vector;
    }
}
