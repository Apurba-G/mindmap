package com.mindmap.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Rate limiting configuration for the API Gateway.
 * Uses Redis-backed rate limiting with IP-based key resolution.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Resolves rate limit keys by client IP address.
     * Uses X-Forwarded-For header if present (behind proxy/LB),
     * otherwise falls back to the remote address.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest()
                .getHeaders().getFirst("X-Forwarded-For");

            String ip;
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                ip = forwardedFor.split(",")[0].trim();
            } else {
                ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            }

            return Mono.just(ip);
        };
    }
}
