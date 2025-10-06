package com.github.matsik.booking.config.rest;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("query.service")
public record QueryRemoteServiceClientProperties(String baseUrl) {
}
