package com.mindmap.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that logs every request and response passing through the gateway.
 * Captures method, path, status code, and latency for observability.
 * Runs first (highest precedence) to wrap the entire filter chain.
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = System.currentTimeMillis();

        String requestId = request.getId();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String clientIp = extractClientIp(request);

        log.info("[{}] → {} {} from {}", requestId, method, path, clientIp);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = response.getStatusCode() != null
                ? response.getStatusCode().value() : 0;

            if (statusCode >= 400) {
                log.warn("[{}] ← {} {} → {} ({}ms)",
                    requestId, method, path, statusCode, duration);
            } else {
                log.info("[{}] ← {} {} → {} ({}ms)",
                    requestId, method, path, statusCode, duration);
            }
        }));
    }

    private String extractClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
            ? request.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
