package com.github.matsik.command;


import com.github.matsik.command.adapter.out.cassandra.BookingPersistenceService;
import com.github.matsik.command.config.cassandra.client.CassandraClientConfiguration;
import com.github.matsik.command.config.cassandra.client.CassandraClientProperties;
import com.github.matsik.command.config.cassandra.mapper.booking.BookingMapperConfiguration;
import com.github.matsik.command.migration.CassandraMigrationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(value = {
        CassandraMigrationService.class,
        CassandraClientConfiguration.class,
        BookingMapperConfiguration.class,
        BookingPersistenceService.class
})
@EnableConfigurationProperties(CassandraClientProperties.class)
public class CassandraAdapterConfig {
}
