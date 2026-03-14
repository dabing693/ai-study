package com.lyh.finance.tools;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
/*
 * @Author:  lengYinHui
 * @Date:  2026/3/14 20:57
 */
@SpringBootTest
public class AkShareToolTest {
    @Autowired
    private AkShareTool akShareTool;
    @Test
    public void getStockValuationTest() {
        String getStockValuation = akShareTool.getStockValuation("600519");
    }
}
