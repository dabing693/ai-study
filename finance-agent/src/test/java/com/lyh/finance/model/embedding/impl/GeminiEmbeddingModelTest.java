package com.lyh.finance.model.embedding.impl;

import com.lyh.finance.model.embedding.EmbeddingModel;
import com.lyh.finance.model.embedding.property.GeminiEmbeddingModelProperty;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class GeminiEmbeddingModelTest {
    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    public void genVectorTest() {
        List<Float> vector = embeddingModel.genVector("你好");
        System.out.println(vector);
    }
}
