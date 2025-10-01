package com.github.matsik.query.booking.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.github.matsik.cassandra.model.BookingByServiceAndDate;
import com.github.matsik.cassandra.model.BookingByUser;
import com.github.matsik.cassandra.model.BookingPartitionKey;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.repository.projection.TimeRange;
import com.github.matsik.query.config.cassandra.client.CassandraClientConfiguration;
import com.github.matsik.query.config.cassandra.client.CassandraClientProperties;
import com.github.matsik.query.config.cassandra.mapper.booking.BookingMapperConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private CqlSession cqlSession;

    @Autowired
    private BookingService service;

    @BeforeAll
    static void setup() throws IOException {
        execMigration();
    }

    @AfterEach
    void tearDown() {
        clearBookingsTable();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetAvailableTimeRangesTestCases")
    void getAvailableTimeRanges(
            String name,
            List<Booking> preTestState,
            GetAvailableTimeRangesQuery query,
            List<TimeRange> expected
    ) {
        // given
        preTestState.forEach(this::persistBooking);

        // when
        List<TimeRange> result = service.getAvailableTimeRanges(query);

        // then
        assertEquals(expected, result);
    }

    private static Stream<Arguments> provideGetAvailableTimeRangesTestCases() {
        return Stream.of(
                Arguments.of(
                        "Should generate all possible booking dates for empty day",
                        List.of(
                        ),
                        getAvailableTimeRangesQuery(120),
                        List.of(
                                TimeRange.Factory.create(0, 120),
                                TimeRange.Factory.create(135, 255),
                                TimeRange.Factory.create(270, 390),
                                TimeRange.Factory.create(405, 525),
                                TimeRange.Factory.create(540, 660),
                                TimeRange.Factory.create(675, 795),
                                TimeRange.Factory.create(810, 930),
                                TimeRange.Factory.create(945, 1065),
                                TimeRange.Factory.create(1080, 1200),
                                TimeRange.Factory.create(1215, 1335)
                        )
                ),
                Arguments.of(
                        "Should find no available time range if all are occupied",
                        List.of(
                                booking(0, 60),
                                booking(75, 135),
                                booking(150, 210),
                                booking(225, 285),
                                booking(300, 360),
                                booking(375, 435),
                                booking(450, 510),
                                booking(525, 585),
                                booking(600, 660),
                                booking(675, 735),
                                booking(750, 810),
                                booking(825, 885),
                                booking(900, 960),
                                booking(975, 1035),
                                booking(1050, 1110),
                                booking(1125, 1185),
                                booking(1200, 1260),
                                booking(1275, 1335),
                                booking(1350, 1410)
                        ),
                        getAvailableTimeRangesQuery(60),
                        List.of(
                        )
                ),
                Arguments.of(
                        "Should round 100 to 120 and find the single available time range",
                        List.of(
                                booking(0, 660),
                                booking(810, 1335)
                        ),
                        getAvailableTimeRangesQuery(100),
                        List.of(
                                TimeRange.Factory.create(675, 795)
                        )
                ),
                Arguments.of(
                        "Should find available time ranges in heterogeneous time ranges",
                        List.of(
                                booking(0, 120),
                                booking(270, 660),
                                booking(945, 1200)
                        ),
                        getAvailableTimeRangesQuery(45),
                        List.of(
                                TimeRange.Factory.create(135, 255),
                                TimeRange.Factory.create(675, 795),
                                TimeRange.Factory.create(810, 930),
                                TimeRange.Factory.create(1215, 1335)
                        )
                )
        );
    }

    private static Booking booking(int start, int end) {
        BookingPartitionKey key = aBookingPartitionKey();
        return newBooking(key.serviceId(), key.date(), UUID.randomUUID(), start, end);
    }

    private static GetAvailableTimeRangesQuery getAvailableTimeRangesQuery(int duration) {
        BookingPartitionKey key = aBookingPartitionKey();
        return GetAvailableTimeRangesQuery.Factory.create(key.date(), key.serviceId(), duration);
    }

    private static BookingPartitionKey aBookingPartitionKey() {
        return BookingPartitionKey.Factory.create(numberToLocalDate(1), numberToUUID(1));
    }

    private static Booking newBooking(UUID serviceId, LocalDate date, UUID bookingId, int start, int end) {
        BookingByServiceAndDate bookingByServiceAndDate = BookingByServiceAndDate.builder()
                .serviceId(serviceId)
                .date(date)
                .bookingId(bookingId)
                .userId(userId())
                .start(start)
                .end(end)
                .build();

        BookingByUser bookingByUser = BookingByUser.builder()
                .userId(userId())
                .serviceId(serviceId)
                .date(date)
                .bookingId(bookingId)
                .start(start)
                .end(end)
                .build();

        return new Booking(
                bookingByServiceAndDate,
                bookingByUser
        );
    }

    private void clearBookingsTable() {
        cqlSession.execute("TRUNCATE booking_system.bookings_by_service_and_date");
        cqlSession.execute("TRUNCATE booking_system.bookings_by_user");
    }

    private void persistBooking(Booking booking) {
        BookingByServiceAndDate bookingByServiceAndDate = booking.bookingByServiceAndDate();

        BoundStatement insertBookingServiceAndDate = cqlSession.prepare(
                "INSERT INTO booking_system.bookings_by_service_and_date " +
                        "(service_id, date, booking_id, user_id, start, end) " +
                        "VALUES (?, ?, ?, ?, ?, ?)"
        ).bind(
                bookingByServiceAndDate.serviceId(),
                bookingByServiceAndDate.date(),
                bookingByServiceAndDate.bookingId(),
                bookingByServiceAndDate.userId(),
                bookingByServiceAndDate.start(),
                bookingByServiceAndDate.end()
        );

        BookingByUser insertBookingByUser = booking.bookingByUser();
        BoundStatement insertUser = cqlSession.prepare(
                "INSERT INTO booking_system.bookings_by_user " +
                        "(user_id, service_id, date, booking_id, start, end) " +
                        "VALUES (?, ?, ?, ?, ?, ?)"
        ).bind(
                insertBookingByUser.userId(),
                insertBookingByUser.serviceId(),
                insertBookingByUser.date(),
                insertBookingByUser.bookingId(),
                insertBookingByUser.start(),
                insertBookingByUser.end()
        );

        BatchStatement batch = BatchStatement.builder(BatchType.LOGGED)
                .addStatement(insertBookingServiceAndDate)
                .addStatement(insertUser)
                .build();

        cqlSession.execute(batch);
    }

    private record Booking(BookingByServiceAndDate bookingByServiceAndDate, BookingByUser bookingByUser) {
    }

    private static UUID userId() {
        return numberToUUID(1);
    }

    public static UUID numberToUUID(long number) {
        String uuidString = String.format("%08d-0000-0000-0000-000000000000", number);
        return UUID.fromString(uuidString);
    }

    public static LocalDate numberToLocalDate(int number) {
        return LocalDate.of(2025, 9, number);
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