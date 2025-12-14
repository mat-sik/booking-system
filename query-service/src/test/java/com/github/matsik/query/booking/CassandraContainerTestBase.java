package com.github.matsik.query.booking;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Testcontainers
public abstract class CassandraContainerTestBase {

    @Container
    private static final CassandraContainer CASSANDRA_CONTAINER = new CassandraContainer("cassandra:5.0.5");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("cassandra.contactPoints", () -> String.format("%s:%d", CASSANDRA_CONTAINER.getHost(), CASSANDRA_CONTAINER.getFirstMappedPort()));
        registry.add("cassandra.keyspaceName", () -> "booking_system");
        registry.add("cassandra.localDatacenter", CASSANDRA_CONTAINER::getLocalDatacenter);
    }

    protected static void execMigration() throws IOException {
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
