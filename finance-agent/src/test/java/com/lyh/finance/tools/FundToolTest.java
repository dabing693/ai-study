package com.lyh.finance.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author lengYinHui
 * @date 2026/1/25
 */
@SpringBootTest
public class FundToolTest {
    @Autowired
    private FundTool fundTool;

    @Test
    public void selectStockTest() {
        String s = fundTool.selectFund("最近一个月收益大于15%");
        System.out.println(s);
    }

    @Test
    public void queryStockTest() {
        String s = fundTool.queryFund("国投瑞银白银期货(LOF)A 近一月收益率");
        System.out.println(s);
    }
}
