package com.lyh.trade.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author lengYinHui
 * @date 2026/1/25
 */
@SpringBootTest
public class StockServiceTest {
    @Autowired
    private StockService stockService;

    @Test
    public void selectStockTest() {
        String s = stockService.selectStock("10点到11点5分钟周期最大成交量前50； 热门板块；");
        System.out.println(s);
    }
    @Test
    public void queryStockTest() {
        String s = stockService.queryStock("贵州茅台2024年总利润");
        System.out.println(s);
    }
}
