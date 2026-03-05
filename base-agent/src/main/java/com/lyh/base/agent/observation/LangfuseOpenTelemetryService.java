package com.lyh.base.agent.observation;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LangfuseOpenTelemetryService {
    @Autowired
    private RestTemplate langfuseRestTemplate;

    @Value("${langfuse.base-url:http://localhost:3001}")
    private String baseUrl;

    public void sendSpan(String traceId, String spanId, String name,
                         Map<String, Object> input, Map<String, Object> output,
                         long startMillis, long endMillis, Exception error) {

        // 构造属性列表
        List<Map<String, Object>> attributes = new ArrayList<>();

        // 添加 Langfuse 特定属性  
        attributes.add(Map.of(
                "key", "langfuse.observation.type",
                "value", Map.of("stringValue", "span")
        ));

        attributes.add(Map.of(
                "key", "langfuse.observation.input",
                "value", Map.of("stringValue", JSONObject.toJSONString(input))
        ));

        attributes.add(Map.of(
                "key", "langfuse.observation.output",
                "value", Map.of("stringValue", JSONObject.toJSONString(output))
        ));

        // 如果有错误，设置错误状态  
        int statusCode = error != null ? 2 : 1; // 2 = ERROR, 1 = OK  

        // 构造 resourceSpans 结构  
        Map<String, Object> resourceSpan = Map.of(
                "resource", Map.of(
                        "attributes", List.of(
                                Map.of("key", "service.name", "value", Map.of("stringValue", "spring-boot-app")),
                                Map.of("key", "telemetry.sdk.language", "value", Map.of("stringValue", "java")),
                                Map.of("key", "telemetry.sdk.name", "value", Map.of("stringValue", "opentelemetry"))
                        )
                ),
                "scopeSpans", List.of(
                        Map.of(
                                "scope", Map.of(
                                        "name", "spring-boot-aop",
                                        "version", "1.0.0"
                                ),
                                "spans", List.of(
                                        Map.of(
                                                "traceId", traceId,
                                                "spanId", spanId,
                                                "name", name,
                                                "kind", 1, // SPAN_KIND_INTERNAL
                                                "startTimeUnixNano", startMillis * 1000000,
                                                "endTimeUnixNano", endMillis * 1000000,
                                                "attributes", attributes,
                                                "status", Map.of("code", statusCode)
                                        )
                                )
                        )
                )
        );

        // 发送到 Langfuse  
        Map<String, Object> payload = Map.of("resourceSpans", List.of(resourceSpan));

        String url = baseUrl + "/api/public/otel/v1/traces";

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload);

        try {
            ResponseEntity<String> response = langfuseRestTemplate.postForEntity(url, request, String.class);
            log.info("响应：{}", response.getBody());
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to send OTel data: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error sending OTel data to Langfuse", e);
        }
    }
}