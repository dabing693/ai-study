package com.lyh.finance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/prompt")
public class PromptController {
    private static final Map<String, String> PROMPT_MAP = new HashMap<>();

    static {
        PROMPT_MAP.put("cls", "请分析这条新闻的市场影响和投资意义。");
        PROMPT_MAP.put("wallstreetcn", "请分析这条新闻的财经影响和投资价值。");
        PROMPT_MAP.put("jin10", "请分析这条快讯对金融市场的短期影响。");
        PROMPT_MAP.put("xueqiu", "请分析该热门股票讨论的投资观点和风险提示。");
        PROMPT_MAP.put("gelonghui", "请分析这条新闻的公司基本面和投资逻辑。");
        PROMPT_MAP.put("fastbull", "请分析这条新闻对相关行业的影响。");
        PROMPT_MAP.put("mktnews", "请分析这条快讯的市场影响。");
    }

    @GetMapping("/news")
    public ResponseEntity<Map<String, String>> getNewsPrompt(@RequestParam("source") String source) {
        Map<String, String> response = new HashMap<>();
        String prompt = PROMPT_MAP.getOrDefault(source.toLowerCase(),
                "请分析这条新闻的要点和投资意义。");
        response.put("source", source);
        response.put("prompt", prompt);
        return ResponseEntity.ok(response);
    }
}
