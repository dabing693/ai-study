package com.lyh.finance.memory;

import com.lyh.base.agent.model.embedding.EmbeddingModel;
import com.lyh.common.util.MarkdownUtil;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.vector.request.AnnSearchReq;
import io.milvus.v2.service.vector.request.HybridSearchReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.request.ranker.WeightedRanker;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@SpringBootTest
public class MilvusSearchReportTest {
    @Value("${milvus.collection-name:finance_agent_memory}")
    private String collectionName;
    @Autowired
    private MilvusClientV2 milvusClientV2;
    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    void generateSearchReport() {
        String query = "如果白酒走弱，你觉得哪个板块2026会走强";
        if (!StringUtils.hasText(query)) {
            log.info("未提供query系统参数，跳过生成报告。");
            return;
        }
        Long maxId = 430L;
        String path = writeSearchReport(query, maxId);
        log.info("检索报告输出路径：{}", path);
    }

    /**
     * 输入一个字符串，输出全文/向量/混合检索结果的Markdown表格报告。
     */
    public String writeSearchReport(String query, Long maxId) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        String filter = buildMaxIdFilter(maxId);
        // 1) 构建全文检索与向量检索请求
        int limit = 10;
        List<Float> queryVector = embeddingModel.genVector(query);
        SearchReq sparseRequest = SearchReq.builder()
                .collectionName(collectionName)
                .annsField("content_embeddings")
                .metricType(IndexParam.MetricType.BM25)
                .data(Collections.singletonList(new EmbeddedText(query)))
                .filter(filter)
                .limit((long) limit)
                .outputFields(Arrays.asList("id", "content"))
                .build();
        // 2) 构建向量检索请求
        SearchReq denseRequest = SearchReq.builder()
                .collectionName(collectionName)
                .annsField("content_vector")
                .metricType(IndexParam.MetricType.COSINE)
                .data(Collections.singletonList(new FloatVec(queryVector)))
                .filter(filter)
                .limit((long) limit)
                .outputFields(Arrays.asList("id", "content"))
                .build();
        // 3) 构建混合检索请求（权重参考原有0.6/0.4）
        HybridSearchReq hybridSearchReq = HybridSearchReq.builder()
                .collectionName(collectionName)
                .searchRequests(Arrays.asList(
                        AnnSearchReq.builder()
                                .vectorFieldName("content_embeddings")
                                .metricType(IndexParam.MetricType.BM25)
                                .vectors(Collections.singletonList(new EmbeddedText(query)))
                                .filter(filter)
                                .limit(limit)
                                .build(),
                        AnnSearchReq.builder()
                                .vectorFieldName("content_vector")
                                .metricType(IndexParam.MetricType.COSINE)
                                .vectors(Collections.singletonList(new FloatVec(queryVector)))
                                .filter(filter)
                                .limit(limit)
                                .build()
                ))
                .ranker(new WeightedRanker(Arrays.asList(0.6f, 0.4f)))
                .limit(limit)
                .outFields(Arrays.asList("id", "content"))
                .build();
        // 4) 执行三种检索并合并结果
        SearchResp sparseResp = milvusClientV2.search(sparseRequest);
        SearchResp denseResp = milvusClientV2.search(denseRequest);
        SearchResp hybridResp = milvusClientV2.hybridSearch(hybridSearchReq);
        Map<Long, SearchRow> rows = new LinkedHashMap<>();
        mergeSearchResults(rows, sparseResp, ScoreType.SPARSE);
        mergeSearchResults(rows, denseResp, ScoreType.DENSE);
        mergeSearchResults(rows, hybridResp, ScoreType.HYBRID);
        normalizeSparseScores(rows);
        String report = String.format("query：%s\n%s", query, buildMarkdownReport(rows));
        // 5) 输出到tmp-data目录
        String prefix = buildFilePrefix(query);
        Path outputDir = Paths.get("..", "tmp-data").normalize();
        try {
            Files.createDirectories(outputDir);
            Path outputPath = outputDir.resolve(prefix + "_" + System.currentTimeMillis() + ".md");
            Files.writeString(outputPath, report, StandardCharsets.UTF_8);
            return outputPath.toString();
        } catch (Exception e) {
            log.warn("写入检索报告失败：{}", e.getMessage());
            return null;
        }
    }

    private void mergeSearchResults(Map<Long, SearchRow> rows, SearchResp resp, ScoreType scoreType) {
        if (resp == null) {
            return;
        }
        if (CollectionUtils.isEmpty(resp.getSearchResults()) ||
                CollectionUtils.isEmpty(resp.getSearchResults().get(0))) {
            return;
        }
        for (SearchResp.SearchResult result : resp.getSearchResults().get(0)) {
            Long id = parseId(result.getId());
            if (id == null) {
                continue;
            }
            SearchRow row = rows.computeIfAbsent(id, SearchRow::new);
            switch (scoreType) {
                case SPARSE:
                    row.sparseScore = result.getScore();
                    break;
                case DENSE:
                    row.denseScore = result.getScore();
                    break;
                case HYBRID:
                    row.hybridScore = result.getScore();
                    break;
                default:
                    break;
            }
            if (row.content == null) {
                row.content = extractContent(result);
            }
        }
    }

    private Long parseId(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String extractContent(SearchResp.SearchResult result) {
        try {
            Method method = result.getClass().getMethod("getEntity");
            Object entity = method.invoke(result);
            if (entity instanceof Map) {
                Object content = ((Map<?, ?>) entity).get("content");
                if (content != null) {
                    return String.valueOf(content);
                }
            }
        } catch (Exception ignored) {
            // 兼容不同版本SDK的字段读取方式
        }
        return null;
    }

    private String buildMarkdownReport(Map<Long, SearchRow> rows) {
        Object[][] table = new Object[rows.size() + 1][5];
        table[0] = new Object[]{"id", "content", "full_score", "vector_score", "hybrid_score"};
        int index = 1;
        for (SearchRow row : rows.values()) {
            table[index++] = new Object[]{
                    row.id,
                    truncateContent(row.content),
                    formatScore(row.sparseScoreNormalized),
                    formatScore(row.denseScore),
                    formatScore(row.hybridScore)
            };
        }
        return MarkdownUtil.arrayToMarkdownTable(table);
    }

    private String formatScore(Float score) {
        if (score == null) {
            return "";
        }
        return String.format(Locale.ROOT, "%.6f", score);
    }

    private String truncateContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.replace("\r", "").replace("\n", " ");
        if (normalized.length() <= 50) {
            return normalized;
        }
        return normalized.substring(0, 50);
    }

    private String buildFilePrefix(String query) {
        String prefix = query.substring(0, Math.min(6, query.length()));
        return prefix.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private String buildMaxIdFilter(Long maxId) {
        if (maxId == null) {
            return null;
        }
        return String.format("id < %d", maxId);
    }

    private enum ScoreType {
        SPARSE,
        DENSE,
        HYBRID
    }

    private static class SearchRow {
        private final Long id;
        private String content;
        private Float sparseScore;
        private Float denseScore;
        private Float hybridScore;
        private Float sparseScoreNormalized;

        private SearchRow(Long id) {
            this.id = id;
        }
    }

    private void normalizeSparseScores(Map<Long, SearchRow> rows) {
        normalizeScoreType(rows, ScoreType.SPARSE);
    }

    private void normalizeScoreType(Map<Long, SearchRow> rows, ScoreType scoreType) {
        Float min = null;
        Float max = null;
        for (SearchRow row : rows.values()) {
            Float score = getScore(row, scoreType);
            if (score == null) {
                continue;
            }
            if (min == null || score < min) {
                min = score;
            }
            if (max == null || score > max) {
                max = score;
            }
        }
        for (SearchRow row : rows.values()) {
            Float score = getScore(row, scoreType);
            Float normalized = normalizeScore(score, min, max);
            setNormalizedScore(row, scoreType, normalized);
        }
    }

    private Float normalizeScore(Float score, Float min, Float max) {
        if (score == null || min == null || max == null) {
            return null;
        }
        if (Float.compare(min, max) == 0) {
            return 1.0f;
        }
        return (score - min) / (max - min);
    }

    private Float getScore(SearchRow row, ScoreType scoreType) {
        switch (scoreType) {
            case SPARSE:
                return row.sparseScore;
            case DENSE:
                return row.denseScore;
            case HYBRID:
                return row.hybridScore;
            default:
                return null;
        }
    }

    private void setNormalizedScore(SearchRow row, ScoreType scoreType, Float value) {
        switch (scoreType) {
            case SPARSE:
                row.sparseScoreNormalized = value;
                break;
            default:
                break;
        }
    }
}
