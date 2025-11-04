package com.github.matsik.booking.config.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class QueryRemoteServiceClientConfiguration {

    @Bean
    public RestClient queryServiceClient(QueryRemoteServiceClientProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultStatusHandler(_ -> true, (_, _) -> {
                })
                .build();
    }

}
