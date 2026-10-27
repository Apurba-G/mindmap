package com.mindmap.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Global error handler for the reactive gateway.
 * Converts exceptions into clean JSON error responses instead of
 * the default Spring Whitelabel error page.
 */
@Slf4j
@Component
@Order(-2) // Run before the default error handler
@RequiredArgsConstructor
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String error;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            error = status.name();
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else if (ex instanceof ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            error = "SERVICE_UNAVAILABLE";
            message = "The requested service is currently unavailable. Please try again later.";
        } else if (ex instanceof TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            error = "GATEWAY_TIMEOUT";
            message = "The service took too long to respond. Please try again.";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            error = "INTERNAL_ERROR";
            message = "An unexpected error occurred. Please try again later.";
        }

        log.error("Gateway error [{}] on {} {}: {}",
            status.value(),
            exchange.getRequest().getMethod(),
            exchange.getRequest().getURI().getPath(),
            ex.getMessage());

        Map<String, Object> body = Map.of(
            "error", error,
            "message", message,
            "status", status.value(),
            "path", exchange.getRequest().getURI().getPath(),
            "timestamp", Instant.now().toString()
        );

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"error\":\"SERIALIZATION_ERROR\"}").getBytes();
        }

        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }
}
