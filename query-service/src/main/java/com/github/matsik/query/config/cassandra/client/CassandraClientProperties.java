package com.github.matsik.query.config.cassandra.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties("cassandra")
public record CassandraClientProperties(
        String contactPoints,
        String keyspaceName,
        String localDatacenter
) {

    public List<InetSocketAddress> contactPointsParsed() {
        return Arrays.stream(contactPoints.split(","))
                .map(contactPoint -> {
                    String[] hostPort = contactPoint.split(":");
                    String host = hostPort[0];
                    int port = Integer.parseInt(hostPort[1]);
                    return new InetSocketAddress(host, port);
                })
                .toList();
    }

}
