package com.github.matsik.booking.config.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("query.service.grpc")
public record QueryRemoteServiceClientProperties(String address) {
}
