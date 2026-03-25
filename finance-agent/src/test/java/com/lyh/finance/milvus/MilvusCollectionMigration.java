package com.lyh.finance.milvus;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.service.utility.request.FlushReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.QueryResp;

import java.util.*;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/25 14:14
 */
public class MilvusCollectionMigration {

    private static final String SOURCE = "finance_agent_memory";
    private static final String TARGET = "agent_memory";
    private static final int BATCH_SIZE = 500;

    public static void main(String[] args) {
        MilvusClientV2 client = new MilvusClientV2(
                ConnectConfig.builder()
                        .uri("http://localhost:19530")
                        .build()
        );
        int offset = 0;
        long total = 0;
        while (true) {
            // 1️⃣ 查询源集合数据
            QueryReq queryReq = QueryReq.builder()
                    .collectionName(SOURCE)
                    .limit(BATCH_SIZE)
                    .offset(offset)
                    .outputFields(Arrays.asList("*")) // 查所有字段
                    .build();
            QueryResp resp = client.query(queryReq);
            List<QueryResp.QueryResult> rows = resp.getQueryResults();
            if (rows == null || rows.isEmpty()) {
                break;
            }
            // 2️⃣ 直接复用数据（字段完全一致）
            List<JsonObject> insertData = new ArrayList<>();
            for (QueryResp.QueryResult row : rows) {
                Gson gson = new Gson();
                row.getEntity().remove("id");
                JsonObject newRow = gson.toJsonTree(row.getEntity()).getAsJsonObject();
                insertData.add(newRow);
            }
            // 3️⃣ 插入目标集合
            InsertReq insertReq = InsertReq.builder()
                    .collectionName(TARGET)
                    .data(insertData)
                    .build();
            client.insert(insertReq);
            total += rows.size();
            offset += BATCH_SIZE;
            System.out.println("已迁移: " + total);

            client.flush(
                    FlushReq.builder()
                            .collectionNames(Arrays.asList(TARGET))
                            .build()
            );
        }
        client.close();
        System.out.println("迁移完成，总条数: " + total);
    }
}