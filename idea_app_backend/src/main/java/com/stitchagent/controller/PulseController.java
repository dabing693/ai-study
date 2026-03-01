package com.stitchagent.controller;

import com.stitchagent.model.PulseMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/pulse")
public class PulseController {

    @GetMapping
    public List<PulseMessage> getPulse() {
        return Arrays.asList(
            PulseMessage.builder()
                .id("1")
                .senderName("贡献者 #402 (匿名)")
                .content("大家好，我已经提交了初始身份验证架构。请 Agent 帮忙检查下安全响应头是否完整？")
                .type("USER")
                .icon("person")
                .build(),
            PulseMessage.builder()
                .id("2")
                .senderName("架构师 Agent")
                .content("正在分析架构。我已经优化了中间件链条。ClaudeCode 分析结果：98% 效率提升")
                .type("AGENT")
                .icon("architecture")
                .codeSnippet("app.use(securityHeaders({\n  contentSecurityPolicy: true,\n  referrerPolicy: 'same-origin'\n}));")
                .build(),
            PulseMessage.builder()
                .id("3")
                .senderName("测试专家")
                .content("发现 JWT 轮转中存在边界情况。正在执行压力测试...")
                .type("EXPERT")
                .icon("biotech")
                .status("12 个待测试场景")
                .build()
        );
    }
}
