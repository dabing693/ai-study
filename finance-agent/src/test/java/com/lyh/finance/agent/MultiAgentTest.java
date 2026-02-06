package com.lyh.finance.agent;

import com.lyh.finance.agent.react.FinanceExpertAgent;
import com.lyh.finance.agent.simple.InvestorAgent;
import com.lyh.finance.tools.AccountTool;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author lengYinHui
 * @date 2026/2/6
 */
@SpringBootTest
public class MultiAgentTest {
    @Resource
    private FinanceExpertAgent financeExpertAgent;
    @Resource
    private InvestorAgent investorAgent;
    @Resource
    private AccountTool accountTool;

    @Test
    public void test() {
        String his = accountTool.getAssetDistribution();
        for (int i = 0; i < 10; i++) {
            String question = investorAgent.send(his);
            String answer = financeExpertAgent.send(question);
            his += "问：" + question + "\n\n";
            his += "答：" + answer + "\n\n";
        }
    }
}
