package com.github.matsik.query.config.cassandra.client;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CassandraClientConfiguration {

    @Bean
    public CqlSession cqlSession(
            CassandraClientProperties cassandraClientProperties
    ) {
        return CqlSession.builder()
                .addContactPoints(cassandraClientProperties.contactPointsParsed())
                .withLocalDatacenter(cassandraClientProperties.localDatacenter())
                .withKeyspace(cassandraClientProperties.keyspaceName())
                .build();
    }

}
