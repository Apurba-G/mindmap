package com.mindmap.profiling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application entry point for the MindMap Psychological Profiling Service.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ProfilingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfilingServiceApplication.class, args);
    }
}
