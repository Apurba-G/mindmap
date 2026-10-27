package com.mindmap.auth.config;

import com.mindmap.auth.service.UserIdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes and synchronizes the Redis Bloom filter on application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterInitializer implements ApplicationRunner {

    private final UserIdService userIdService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting Redis Bloom filter initialization for User IDs...");
        try {
            userIdService.rebuildBloomFilter();
            log.info("Redis Bloom filter initialization completed successfully.");
        } catch (Exception e) {
            log.warn("Could not complete Bloom filter initialization (RedisBloom module may be offline): {}", e.getMessage());
        }
    }
}
