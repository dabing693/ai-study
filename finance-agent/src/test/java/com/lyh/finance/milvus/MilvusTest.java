package com.lyh.finance.milvus;

import io.milvus.client.MilvusServiceClient;
import io.milvus.v2.client.MilvusClientV2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MilvusTest {
    @Autowired
    private MilvusServiceClient client;
    @Autowired
    private MilvusClientV2 milvusClientV2;

    @Test
    public void milvusTest() {

    }
}
