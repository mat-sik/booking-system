package com.github.matsik.command.config.cassandra;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("cassandra")
public record CassandraClientProperties(
        String contactPoints,
        String keyspaceName,
        String datacenter
) {
}
