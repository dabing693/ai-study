package com.lyh.finance.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
public class MemoryQuery {
    private String conversationId;
    private Integer limit;
    private String query;
}
