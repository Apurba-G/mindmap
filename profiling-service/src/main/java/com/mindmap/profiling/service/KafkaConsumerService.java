package com.mindmap.profiling.service;

import com.mindmap.profiling.dto.AssessmentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka listener service consuming 'assessment.completed' events and dispatching them to {@link ProfileService}.
 * If processing fails, the failed payload and metadata are routed to the Dead Letter Topic (DLT).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final ProfileService profileService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${profiling.kafka.topics.assessment-completed-dlt:assessment.completed.DLT}")
    private String deadLetterTopic;

    /**
     * Consumes assessment completion events from the Kafka topic.
     *
     * @param event     the assessment event payload
     * @param partition partition id
     * @param offset    record offset
     */
    @KafkaListener(
            topics = "${profiling.kafka.topics.assessment-completed:assessment.completed}",
            groupId = "${spring.kafka.consumer.group-id:profiling-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAssessmentCompleted(
            @Payload AssessmentCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Received AssessmentCompletedEvent: userId={}, level={}, session={}, partition={}, offset={}",
                event.userId(), event.level(), event.sessionId(), partition, offset);

        try {
            profileService.processAssessmentCompleted(event);
            log.info("Successfully processed AssessmentCompletedEvent for userId={}", event.userId());
        } catch (Exception ex) {
            log.error("Error processing AssessmentCompletedEvent for userId={}. Routing to DLT topic {}: {}",
                    event.userId(), deadLetterTopic, ex.getMessage(), ex);
            sendToDeadLetterTopic(event, partition, offset, ex);
        }
    }

    private void sendToDeadLetterTopic(AssessmentCompletedEvent originalEvent, int partition, long offset, Exception ex) {
        try {
            Map<String, Object> dltPayload = new HashMap<>();
            dltPayload.put("originalEvent", originalEvent);
            dltPayload.put("failedPartition", partition);
            dltPayload.put("failedOffset", offset);
            dltPayload.put("errorMessage", ex.getMessage());
            dltPayload.put("errorType", ex.getClass().getName());
            dltPayload.put("failedTimestamp", System.currentTimeMillis());

            kafkaTemplate.send(deadLetterTopic, originalEvent.userId().toString(), dltPayload);
            log.warn("Routed failed event for userId={} to DLT: {}", originalEvent.userId(), deadLetterTopic);
        } catch (Exception dltEx) {
            log.error("Fatal: failed to route message to DLT for userId={}: {}", originalEvent.userId(), dltEx.getMessage(), dltEx);
        }
    }
}
