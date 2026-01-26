package com.lyh.trade.plansolve;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author lengYinHui
 * @date 2026/1/26
 */
@Slf4j
@Component
public class Executor {
    private static final String EXECUTOR_PROMPT = """
            你是一位顶级的AI执行专家。你的任务是严格按照给定的计划，一步步地解决问题。
            你将收到原始问题、完整的计划、以及到目前为止已经完成的步骤和结果。
            请你专注于解决“当前步骤”，并仅输出该步骤的最终答案，不要输出任何额外的解释或对话。

            # 原始问题:
            {question}

            # 完整计划:
            {plan}

            # 历史步骤与结果:
            {history}

            # 当前步骤:
            {current_step}

            请仅输出针对“当前步骤”的回答:
            """;
    private static final PromptTemplate EXECUTOR_PROMPT_TEMPLATE = new PromptTemplate(EXECUTOR_PROMPT);
    @Resource(name = "chatClient")
    private ChatClient noLoggerChatClient;

    public String execute(String question, List<String> planList) {
        String history = "";
        log.info("\n--- 正在执行计划 ---");
        String finalOutput = "";
        for (int i = 0; i < planList.size(); i++) {
            String step = planList.get(i);
            Prompt prompt = EXECUTOR_PROMPT_TEMPLATE.create(Map.of(
                    "question", question,
                    "plan", planList,
                    "history", Objects.equals(history, "") ? "空" : history,
                    "current_step", step
            ));
            String output = noLoggerChatClient.prompt(prompt).call().content();
            if (i == planList.size() - 1) {
                finalOutput = output;
            }
            String stepInfo = String.format("步骤 %s: %s\n结果: %s\n\n", i + 1, step, output);
            history += stepInfo;
            log.info(stepInfo);
        }
        return finalOutput;
    }
}
