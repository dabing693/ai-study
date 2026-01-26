package com.lyh.trade.plansolve;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author lengYinHui
 * @date 2026/1/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanAndSolveAgent {
    private final Planner planner;
    private final Executor executor;

    public void run(String question) {
        log.info("--- 开始处理问题 ---\n问题: {}", question);
        List<String> planList = planner.plan(question);
        if (CollectionUtils.isEmpty(planList)) {
            return;
        }
        String output = executor.execute(question, planList);
        log.info("--- 任务完成 ---\n最终答案: {}", output);
    }
}
