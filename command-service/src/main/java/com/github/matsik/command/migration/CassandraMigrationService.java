package com.github.matsik.command.migration;

import com.datastax.oss.driver.api.core.CqlSession;
import com.github.matsik.command.config.cassandra.client.CassandraClientProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CassandraMigrationService {

    private final CassandraClientProperties cassandraClientProperties;

    private final List<String> migrationFiles = List.of(
            "db/migrations/V0__migrations.cql",
            "db/migrations/V1__bookings.cql"
    );

    public void runMigrations() {
        try (CqlSession systemSession = createSystemSession()) {
            for (String migrationFileName : migrationFiles) {
                log.info("Executing migration file: {}", migrationFileName);
                Optional<String> persistedChecksum = isMigrationTableInitializationScript(migrationFileName) ?
                        Optional.empty() : isMigrationExecuted(systemSession, migrationFileName);

                String calculatedChecksum = checksum(migrationFileName);

                if (persistedChecksum.isPresent() && !Objects.equals(persistedChecksum.get(), calculatedChecksum)) {
                    throw new IllegalStateException(String.format("Checksums do not match, fileName: %s", migrationFileName));
                } else if (persistedChecksum.isEmpty()) {
                    execMigration(systemSession, migrationFileName);
                    markMigrationAsExecuted(systemSession, migrationFileName, calculatedChecksum);
                    log.info("Executed migration file: {}", migrationFileName);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private CqlSession createSystemSession() {
        return CqlSession.builder()
                .addContactPoints(cassandraClientProperties.contactPointsParsed())
                .withLocalDatacenter(cassandraClientProperties.localDatacenter())
                .build();
    }

    private String checksum(String fileName) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] migrationFileContents = resourceBytes(fileName);

        return HexFormat.of().formatHex(digest.digest(migrationFileContents));
    }

    private boolean isMigrationTableInitializationScript(String fileName) {
        return Objects.equals(fileName, migrationFiles.getFirst());
    }

    private void execMigration(CqlSession session, String fileName) throws IOException {
        String migrationScriptCql = new String(resourceBytes(fileName), StandardCharsets.UTF_8);
        String[] statements = migrationScriptCql.split(";");
        Arrays.stream(statements).forEach(session::execute);
    }

    private void markMigrationAsExecuted(CqlSession session, String fileName, String fileChecksum) {
        String insertCql = """
                INSERT INTO migration.schema_migrations (fileName, executed_at, checksum)
                VALUES (?, toTimestamp(now()), ?);
                """;
        session.execute(insertCql, fileName, fileChecksum);
    }

    private Optional<String> isMigrationExecuted(CqlSession session, String fileName) {
        String checkCql = "SELECT checksum FROM migration.schema_migrations WHERE fileName = ?";
        return Optional.ofNullable(session.execute(checkCql, fileName).one())
                .map(row -> row.getString("checksum"));
    }

    private byte[] resourceBytes(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(fileName);
        return resource.getInputStream().readAllBytes();
    }

}
