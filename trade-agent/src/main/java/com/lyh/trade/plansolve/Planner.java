package com.lyh.trade.plansolve;

import com.alibaba.fastjson2.JSONArray;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/1/26
 */
@Slf4j
@Component
public class Planner {
    @Resource(name = "chatClient")
    private ChatClient noLoggerChatClient;

    private static final String PLANNER_PROMPT_TEMPLATE = """
            你是一个顶级的AI规划专家。你的任务是将用户提出的复杂问题分解成一个由多个简单步骤组成的行动计划。
            请确保计划中的每个步骤都是一个独立的、可执行的子任务，并且严格按照逻辑顺序排列。
            你的输出必须是一个Python列表，其中每个元素都是一个描述子任务的字符串。

            问题: {question}

            请严格按照以下格式输出你的计划,```python与```作为前后缀是必要的:
            ```python
            ["步骤1", "步骤2", "步骤3", ...]
            ```
            """;

    public List<String> plan(String task) {
        String input = PLANNER_PROMPT_TEMPLATE.replace("{question}", task);
        String output = noLoggerChatClient.prompt(input).call().content();
        if (!StringUtils.hasLength(output)) {
            return Collections.emptyList();
        }
        String listStr = output.replace("```python", "").replace("```", "");
        log.info("规划步骤: \n{}", listStr);
        return JSONArray.parseArray(listStr, String.class);
    }
}
