package com.github.matsik.command.client.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kafka")
public record KafkaClientProperties(String clientId, String hosts, String groupId) {
}
