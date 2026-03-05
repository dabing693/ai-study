package com.lyh.base.agent.observation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/4 21:50
 */
@Service
public class LangfuseTraceService {

    @Autowired
    private RestTemplate langfuseRestTemplate;

    @Value("${langfuse.base-url}")
    private String baseUrl;

    public void createTrace(String traceId, String traceName, Map<String, Object> input, Map<String, Object> output) {
        Map<String, Object> traceData = new HashMap<>();
        traceData.put("id", traceId);
        traceData.put("name", traceName);
        traceData.put("input", input);
        traceData.put("output", output);
        traceData.put("timestamp", Instant.now().toString());

        String url = baseUrl + "/api/public/traces";
        langfuseRestTemplate.postForObject(url, traceData, String.class);
    }
}