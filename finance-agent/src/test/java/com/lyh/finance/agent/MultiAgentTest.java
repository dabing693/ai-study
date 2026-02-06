package com.lyh.finance.agent;

import com.lyh.finance.agent.react.FinanceExpertAgent;
import com.lyh.finance.agent.simple.InvestorAgent;
import com.lyh.finance.tools.AccountTool;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;

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
    Map<String, Callable<String>> map = new HashMap<>() {
        {
            put("资产分布", () -> accountTool.getAssetDistribution());
            put("股票持仓", () -> accountTool.getStockHoldings());
            put("账户区间收益", () -> accountTool.getAccountProfit());
            put("账户交易", () -> accountTool.getAccountTransactions("", ""));
        }
    };


    String prompt = """
            你账户的{title}数据如下：
            {data}
            观察上面的数据，提出你的问题以供专家回答。专家回答后，你可以针对其回答追问，或者提出新的问题。
            1、每次只提一个问题；
            2、每个问题尽量简短，30字以内；
            """;

    @Test
    public void test() throws Exception {
        String his = "";
        for (int i = 0; i < 10; i++) {
            String append = "".equals(his) ? "" : "历史聊天记录：\n" + his;
            String question = investorAgent.send(prompt() + append);
            String q = String.format("问题%s：", i + 1) + question + "\n\n";
            System.out.println(q);
            String answer = financeExpertAgent.send(question);
            String a = String.format("回答%s：", i + 1) + answer + "\n\n";
            System.out.println(a);
            his += (q + a);
        }
    }

    private String prompt() throws Exception {
        List<String> list = new ArrayList<>(map.keySet());
        String title = list.get(new Random().nextInt(1000) % 4);
        return prompt.replace("{title}", title)
                .replace("{data}", map.get(title).call());
    }
}
