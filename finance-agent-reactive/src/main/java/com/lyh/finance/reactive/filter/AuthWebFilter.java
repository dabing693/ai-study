package com.lyh.finance.reactive.filter;

import com.lyh.finance.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.nio.charset.StandardCharsets;

/**
 * @author claude code with kimi
 * @date 2026/2/6
 */
@Component
// 确保过滤器尽早执行，数值越小优先级越高
@Order(-1)
public class AuthWebFilter implements WebFilter {

    @Autowired
    private JwtUtil jwtUtil;

    //用于在Reactor Context 中传递用户ID的 key
    private static final String USER_ID_CONTEXT_KEY = "userId";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        // 允许跨域预检请求通过
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        // 获取 Authorization Header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String errorBody = "{\"error\":\"未登录或token格式错误\"}";
            byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtUtil.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String errorBody = "{\"error\":\"token无效或已过期\"}";
            byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        }

        // 解析 Token 中的用户信息
        Long userId = jwtUtil.getUserIdFromToken(token);
        // 将用户信息存入 exchange attributes，供后续使用
        exchange.getAttributes().put(USER_ID_CONTEXT_KEY, userId);

        // 继续过滤器链，并将用户信息放入 Reactor Context（供后续异步操作使用）
        return chain.filter(exchange)
                .contextWrite(Context.of(USER_ID_CONTEXT_KEY, userId));
    }

    /**
     * 从 Reactor Context 中获取当前用户ID（用于 Service 层或其他异步操作）
     */
    public static Mono<Long> getCurrentUserId() {
        return Mono.deferContextual(ctx ->
                ctx.hasKey(USER_ID_CONTEXT_KEY)
                        ? Mono.just(ctx.get(USER_ID_CONTEXT_KEY))
                        : Mono.empty()
        );
    }
}