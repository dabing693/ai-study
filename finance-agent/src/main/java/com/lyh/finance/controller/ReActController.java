package com.lyh.finance.controller;

import com.lyh.finance.context.RequestContext;
import com.lyh.finance.agent.ReActAgent;
import com.lyh.finance.domain.ChatResponse;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * @author lengYinHui
 * @date 2026/2/3
 */
@RestController
@RequestMapping("/react")
public class ReActController {
    @Resource
    private ReActAgent reActAgent;

    @GetMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestParam("query") String query,
                                             @RequestHeader(value = "sessionId", required = false) String sessionId) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        RequestContext.setSession(sessionId);
        ChatResponse response = reActAgent.chat(query);
        RequestContext.clear();
        //将sessionId放入响应头
        return ResponseEntity.ok()
                .header("X-Session-Id", sessionId)
                .body(response);
    }
}
