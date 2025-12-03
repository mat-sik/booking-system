package com.github.matsik.command.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kafka")
public record KafkaProperties(
        KafkaProperties.Client client,
        KafkaProperties.Consumer consumer,
        KafkaProperties.TopicDefaults topics
) {
    public record Client(
            String clientId,
            String bootstrapServers
    ) {
    }

    public record Consumer(
            String groupId,
            int concurrentConsumerCount,
            long pollTimeoutMs,
            int maxPollIntervalMs,
            int maxPollRecords
    ) {
    }

    public record TopicDefaults(
            String bookingTopicName,
            int partitions,
            short replicationFactor
    ) {
    }
}
