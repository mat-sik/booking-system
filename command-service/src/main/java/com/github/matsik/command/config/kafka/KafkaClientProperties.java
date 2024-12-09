package com.github.matsik.command.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kafka")
public record KafkaClientProperties(String clientId, String hosts, String groupId) {
}
