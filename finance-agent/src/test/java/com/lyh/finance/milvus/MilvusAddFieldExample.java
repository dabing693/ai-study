package com.lyh.finance.milvus;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.common.DataType;
import io.milvus.v2.service.collection.request.AddCollectionFieldReq;

public class MilvusAddFieldExample {

    public static void main(String[] args) {

        // 1. 创建 MilvusClient
        MilvusClientV2 client = new MilvusClientV2(
                ConnectConfig.builder()
                        .uri("http://localhost:19530") // Milvus 地址
                        .build()
        );

        // 2. 构造请求
        AddCollectionFieldReq req = AddCollectionFieldReq.builder()
                .collectionName("finance_agent_memory")
                .fieldName("msg_id")
                .dataType(DataType.Int64)
                .isNullable(true)
                // 可选：设置默认值
                // .defaultValue(0L)
                // 可选：字段描述
                // .description("创建时间戳")
                .build();

        // 3. 执行添加字段
        client.addCollectionField(req);


        // 5. 关闭客户端
        client.close();
    }
}