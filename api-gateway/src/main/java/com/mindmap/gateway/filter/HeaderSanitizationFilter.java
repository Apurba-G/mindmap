package com.mindmap.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Strips any externally-set X-User-* headers from incoming requests
 * to prevent header injection attacks. Only the JwtAuthFilter may set
 * these headers after validating the JWT token.
 */
@Slf4j
@Component
public class HeaderSanitizationFilter implements GlobalFilter, Ordered {

    private static final String[] INTERNAL_HEADERS = {
        "X-User-Id",
        "X-User-Handle",
        "X-User-Roles"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

        for (String header : INTERNAL_HEADERS) {
            if (exchange.getRequest().getHeaders().containsKey(header)) {
                log.warn("Stripping externally-set internal header '{}' from request to {}",
                    header, exchange.getRequest().getURI().getPath());
                requestBuilder.headers(h -> h.remove(header));
            }
        }

        ServerHttpRequest sanitizedRequest = requestBuilder.build();
        return chain.filter(exchange.mutate().request(sanitizedRequest).build());
    }

    @Override
    public int getOrder() {
        // Run BEFORE the JWT filter but AFTER the logging filter
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
