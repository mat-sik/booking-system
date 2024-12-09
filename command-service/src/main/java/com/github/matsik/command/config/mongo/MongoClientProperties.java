package com.github.matsik.command.config.mongo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("mongo")
public record MongoClientProperties(
        String host,
        int port,
        String login,
        char[] password,
        String authDatabase,
        String database) {
}
