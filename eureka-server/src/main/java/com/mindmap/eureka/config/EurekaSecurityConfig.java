package com.mindmap.eureka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Eureka dashboard security configuration.
 * In production, enable HTTP Basic auth for the Eureka dashboard
 * to prevent unauthorized service discovery browsing.
 * In dev, all endpoints are open for convenience.
 */
@Configuration
public class EurekaSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain eurekaSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Eureka clients use REST, not forms
            .authorizeHttpRequests(auth -> auth
                // Actuator health is always public (for Docker healthchecks)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Eureka peer replication endpoint
                .requestMatchers("/eureka/**").permitAll()
                // Everything else (dashboard) is open in dev
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
