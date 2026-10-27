package com.mindmap.assessment.service;

import com.mindmap.assessment.dto.AssessmentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for publishing assessment lifecycle events onto Apache Kafka topics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topics.assessment-completed:assessment.completed}")
    private String completedTopic;

    /**
     * Publishes an AssessmentCompletedEvent to Kafka topic 'assessment.completed'.
     *
     * @param event the event payload containing user answers and metadata
     */
    public void publishAssessmentCompleted(AssessmentCompletedEvent event) {
        String key = event.userId().toString();
        log.info("Publishing AssessmentCompletedEvent to topic '{}' with key '{}', session '{}'",
                completedTopic, key, event.sessionId());

        kafkaTemplate.send(completedTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published AssessmentCompletedEvent for session {} at offset {}",
                                event.sessionId(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to publish AssessmentCompletedEvent for session {}",
                                event.sessionId(), ex);
                    }
                });
    }
}
