package com.lyh.base.agent.observation;

import com.alibaba.fastjson2.JSONObject;
import com.lyh.base.agent.context.RequestContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
public class LangfuseTraceAspect {
    @Autowired
    private LangfuseTraceService traceService;
    @Autowired
    private LangfuseOpenTelemetryService langfuseOpenTelemetryService;

    @Around("@annotation(com.lyh.base.agent.observation.LangfuseTracer)")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String clzName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String traceName = String.format("%s::%s", clzName, methodName);
        String traceId = RequestContext.getSession();

        Map<String, Object> input = new HashMap<>();
        input.put("method", traceName);
        input.put("args", Arrays.toString(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            Map<String, Object> output = Map.of("result", JSONObject.toJSONString(result));
            traceService.createTrace(traceId, traceName, input, output);
            return result;
        } catch (Exception e) {
            Map<String, Object> errorOutput = Map.of("result", e.getMessage());
            traceService.createTrace(traceId, traceName + "_error", input, errorOutput);
            throw e;
        }
    }

    @Around("@annotation(com.lyh.base.agent.observation.LangfuseObserver)")
    public Object observeMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String clzName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String traceName = String.format("%s::%s", clzName, methodName);
        String traceId = RequestContext.getSession();

        Map<String, Object> input = new HashMap<>();
        input.put("method", traceName);
        input.put("args", Arrays.toString(joinPoint.getArgs()));
        String spanId = UUID.randomUUID().toString();
        try {
            Object result = joinPoint.proceed();
            Map<String, Object> output = Map.of("result", JSONObject.toJSONString(result));
            langfuseOpenTelemetryService.sendSpan(traceId, spanId, traceName,
                    input, output,
                    start, System.currentTimeMillis(), null
            );
            return result;
        } catch (Exception e) {
            langfuseOpenTelemetryService.sendSpan(traceId, spanId, traceName,
                    input, null,
                    start, System.currentTimeMillis(), e);
            throw e;
        }
    }
}