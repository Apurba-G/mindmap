package com.mindmap.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the MindMap Auth & Identity Service.
 * Provides user registration, Bloom-filter-backed UserID generation,
 * Argon2 password & recovery paragraph hashing, JWT issuance, and audit logging.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
