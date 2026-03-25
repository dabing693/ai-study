package com.lyh.finance.milvus;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.QueryResp;

import java.util.*;

public class MilvusDataMigration {

    private static final String COLLECTION = "finance_agent_memory";
    private static final int BATCH_SIZE = 1000;

    public static void main(String[] args) {

        MilvusClientV2 client = new MilvusClientV2(
                ConnectConfig.builder()
                        .uri("http://localhost:19530")
                        .build()
        );

        int offset = 0;

        while (true) {

            // 1️⃣ 分页查询
            QueryReq queryReq = QueryReq.builder()
                    .collectionName(COLLECTION)
                    .limit(BATCH_SIZE)
                    .offset(offset)
                    .outputFields(Arrays.asList("*")) // 查所有字段
                    .build();

            QueryResp resp = client.query(queryReq);
            List<QueryResp.QueryResult> rows = resp.getQueryResults();

            if (rows == null || rows.isEmpty()) {
                break;
            }

            // 2️⃣ 构造新数据（复制 + 增加 msg_id）
            List<JsonObject> newRows = new ArrayList<>();

            for (QueryResp.QueryResult row : rows) {
                Gson gson = new Gson();
                //核心逻辑：id -> msg_id
                row.getEntity().put("msg_id", row.getEntity().get("id"));
                JsonObject newRow = gson.toJsonTree(row.getEntity()).getAsJsonObject();
                newRows.add(newRow);
            }

            // 3️⃣ 插入回去（⚠️ 如果是同一个集合，会重复数据！）
            InsertReq insertReq = InsertReq.builder()
                    .collectionName(COLLECTION)
                    .data(newRows)
                    .build();
            client.insert(insertReq);
            offset += BATCH_SIZE;
            System.out.println("Processed: " + offset);
        }

        client.close();
    }
}