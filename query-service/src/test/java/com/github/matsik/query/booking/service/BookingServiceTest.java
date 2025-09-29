package com.github.matsik.query.booking.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.github.matsik.cassandra.model.BookingPartitionKey;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.config.cassandra.client.CassandraClientConfiguration;
import com.github.matsik.query.config.cassandra.client.CassandraClientProperties;
import com.github.matsik.query.config.cassandra.mapper.booking.BookingMapperConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

@SpringBootTest(classes = {
        BookingServiceTest.TestCassandraConfig.class,
        CassandraClientConfiguration.class,
        BookingMapperConfiguration.class,
        BookingService.class
})
@Testcontainers
class BookingServiceTest {

    @Container
    private static final CassandraContainer CASSANDRA_CONTAINER = new CassandraContainer("cassandra:5.0.5");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("cassandra.contactPoints", () -> String.format("%s:%d", CASSANDRA_CONTAINER.getHost(), CASSANDRA_CONTAINER.getFirstMappedPort()));
        registry.add("cassandra.keyspaceName", () -> "booking_system");
        registry.add("cassandra.localDatacenter", CASSANDRA_CONTAINER::getLocalDatacenter);
    }

    @Configuration
    @EnableConfigurationProperties(CassandraClientProperties.class)
    public static class TestCassandraConfig {
    }

    @Autowired
    private BookingService service;

    @BeforeAll
    static void setup() throws IOException {
        execMigration();
    }

    @Test
    void getAvailableTimeRanges() {
        BookingPartitionKey key = BookingPartitionKey.Factory.create(LocalDate.of(2025, 10, 1), UUID.randomUUID());
        GetAvailableTimeRangesQuery query = new GetAvailableTimeRangesQuery(key, 30);
        service.getAvailableTimeRanges(query);
    }

    private static void execMigration() throws IOException {
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(
                        CASSANDRA_CONTAINER.getHost(),
                        CASSANDRA_CONTAINER.getFirstMappedPort()
                ))
                .withLocalDatacenter(CASSANDRA_CONTAINER.getLocalDatacenter())
                .build()
        ) {
            execMigration(session, "schema.cql");
        }
    }

    private static void execMigration(CqlSession session, String fileName) throws IOException {
        String migrationScriptCql = new String(resourceBytes(fileName), StandardCharsets.UTF_8);
        String[] statements = migrationScriptCql.split(";");
        Arrays.stream(statements).forEach(session::execute);
    }

    private static byte[] resourceBytes(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(fileName);
        return resource.getInputStream().readAllBytes();
    }

}