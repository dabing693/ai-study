package com.lyh.trade.tools;

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
    private SearchService searchService;

    @Test
    public void searchTest() {
        final String r = searchService.search("国投瑞银白银期货(LOF)A，最近有啥利好消息吗");
        System.out.println(r);
    }
}
