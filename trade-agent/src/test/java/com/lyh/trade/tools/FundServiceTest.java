package com.lyh.trade.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author lengYinHui
 * @date 2026/1/25
 */
@SpringBootTest
public class FundServiceTest {
    @Autowired
    private FundService fundService;

    @Test
    public void selectStockTest() {
        String s = fundService.selectFund("最近一个月收益大于15%");
        System.out.println(s);
    }
    @Test
    public void queryStockTest() {
        String s = fundService.queryFund("国投瑞银白银期货(LOF)A 近一月收益率");
        System.out.println(s);
    }
}
