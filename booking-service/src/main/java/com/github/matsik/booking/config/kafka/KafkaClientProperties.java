package com.github.matsik.booking.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("kafka")
public record KafkaClientProperties(String hosts, String clientId) {
}
