package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.ProfileGeneratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service producing Kafka events when psychological profiles are generated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${profiling.kafka.topics.profile-generated:profile.generated}")
    private String profileGeneratedTopic;

    /**
     * Publishes a {@link ProfileGeneratedEvent} to the Kafka topic.
     *
     * @param event the event payload
     */
    public void publishProfileGenerated(ProfileGeneratedEvent event) {
        log.info("Publishing ProfileGeneratedEvent for userId={}, level={}, profileId={}",
                event.userId(), event.level(), event.profileId());

        kafkaTemplate.send(profileGeneratedTopic, event.userId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published ProfileGeneratedEvent for userId={} to topic={} partition={} offset={}",
                                event.userId(),
                                profileGeneratedTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish ProfileGeneratedEvent for userId={}: {}",
                                event.userId(), ex.getMessage(), ex);
                    }
                });
    }
}
