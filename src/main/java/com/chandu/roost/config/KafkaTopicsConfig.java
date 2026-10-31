package com.chandu.roost.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the incident-events topic so it is auto-created on a dev broker.
 * In production the topic is usually pre-provisioned with a fixed partition
 * count; partitions are what give us ordering-per-key and parallelism.
 */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    NewTopic incidentsTopic(
            @Value("${roost.events.topic}") String topic,
            @Value("${roost.events.partitions:3}") int partitions) {

        // One replica is fine for single-node dev; bump in prod via broker config.
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(1)
                .build();
    }
}
