package com.lyh.trade.controller;

import com.lyh.trade.self_react.SelfReActAgent;
import com.lyh.trade.self_react.domain.ChatResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@RestController
@RequestMapping("/self_react")
public class SelfReActController {
    @Resource
    private SelfReActAgent selfReActAgent;

    @GetMapping("/chat")
    public ChatResponse chat(@RequestParam("query") String query,
                             @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        ChatResponse response = selfReActAgent.chat(query, sessionId);
        return response;
    }
}
