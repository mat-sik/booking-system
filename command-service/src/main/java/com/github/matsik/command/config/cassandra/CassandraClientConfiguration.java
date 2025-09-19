package com.github.matsik.command.config.cassandra;

import com.github.matsik.command.migration.CassandraMigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;

@Configuration
@RequiredArgsConstructor
public class CassandraClientConfiguration extends AbstractCassandraConfiguration {

    private final CassandraClientProperties cassandraClientProperties;
    private final CassandraMigrationService cassandraMigrationService;

    @Override
    public String getContactPoints() {
        cassandraMigrationService.runMigrations();
        return cassandraClientProperties.contactPoints();
    }

    @Override
    public String getKeyspaceName() {
        return cassandraClientProperties.keyspaceName();
    }

    @Override
    public String getLocalDataCenter() {
        return cassandraClientProperties.datacenter();
    }

}
