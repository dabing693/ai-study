package com.lyh.trade.controller;

import com.lyh.trade.self_react.RequestContext;
import com.lyh.trade.self_react.SelfReActAgent;
import com.lyh.trade.self_react.domain.ChatResponse;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ChatResponse> chat(@RequestParam("query") String query,
                                             @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        RequestContext.setSession(sessionId);
        ChatResponse response = selfReActAgent.chat(query);
        RequestContext.clear();
        //将sessionId放入响应头
        return ResponseEntity.ok()
                .header("X-Session-Id", sessionId)
                .body(response);
    }
}
