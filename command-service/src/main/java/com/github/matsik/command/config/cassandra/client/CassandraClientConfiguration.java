package com.github.matsik.command.config.cassandra.client;

import com.datastax.oss.driver.api.core.CqlSession;
import com.github.matsik.command.migration.CassandraMigrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CassandraClientConfiguration {

    @Bean
    public CqlSession cqlSession(
            CassandraMigrationService cassandraMigrationService,
            CassandraClientProperties cassandraClientProperties
    ) {
        cassandraMigrationService.runMigrations();

        return CqlSession.builder()
                .addContactPoints(cassandraClientProperties.contactPointsParsed())
                .withLocalDatacenter(cassandraClientProperties.localDatacenter())
                .withKeyspace(cassandraClientProperties.keyspaceName())
                .build();
    }

}
