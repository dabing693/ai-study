package com.lyh.finance.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author lengYinHui
 * @date 2026/1/26
 */
@SpringBootTest
public class SearchServiceTest {
    @Resource
    private SearchTool searchTool;

    @Test
    public void searchTest() {
        final String r = searchTool.search("贵州茅台 利好消息 最新", "2026-02-01");
        System.out.println(r);
    }
}
