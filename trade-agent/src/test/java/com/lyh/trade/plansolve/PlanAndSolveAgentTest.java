package com.lyh.trade.plansolve;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author lengYinHui
 * @date 2026/1/26
 */
@SpringBootTest
public class PlanAndSolveAgentTest {
    @Autowired
    private PlanAndSolveAgent planAndSolveAgent;

    @Test
    public void runTest() {
        String q1 = "我的持仓中，哪个股票赚得最多？";
        String q2 = "我的持仓中赚得最多的股票，最近有啥利空消息吗";
        planAndSolveAgent.run(q2);
    }
}
