package com.github.matsik.query.booking.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.github.matsik.cassandra.model.BookingByServiceAndDate;
import com.github.matsik.cassandra.model.BookingByUser;
import com.github.matsik.cassandra.model.BookingPartitionKey;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetFirstUserBookingsQuery;
import com.github.matsik.query.booking.query.GetNextUserBookingsQuery;
import com.github.matsik.query.booking.query.GetUserBookingQuery;
import com.github.matsik.query.booking.query.GetUserBookingsQuery;
import com.github.matsik.query.booking.repository.projection.TimeRange;
import com.github.matsik.query.booking.repository.projection.UserBooking;
import com.github.matsik.query.booking.service.exception.UserBookingNotFoundException;
import com.github.matsik.query.config.cassandra.client.CassandraClientConfiguration;
import com.github.matsik.query.config.cassandra.client.CassandraClientProperties;
import com.github.matsik.query.config.cassandra.mapper.booking.BookingMapperConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void getAvailableTimeRangesTest(
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
                        "Should generate all possible booking dates for an empty day",
                        List.of(
                        ),
                        getAvailableTimeRangesQuery(60),
                        timeRangesForEmptyDay(60)
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
                                booking(775, 1440)
                        ),
                        getAvailableTimeRangesQuery(45),
                        List.of(
                                TimeRange.Factory.create(135, 195),
                                TimeRange.Factory.create(150, 210),
                                TimeRange.Factory.create(165, 225),
                                TimeRange.Factory.create(180, 240),
                                TimeRange.Factory.create(195, 255),
                                TimeRange.Factory.create(675, 735),
                                TimeRange.Factory.create(690, 750)
                        )
                )
        );
    }

    private static List<TimeRange> timeRangesForEmptyDay(int serviceDuration) {
        List<TimeRange> timeRanges = new ArrayList<>();
        for (int start = 0; start <= 24 * 60 - serviceDuration; start += 15) {
            timeRanges.add(new TimeRange(start, start + serviceDuration));
        }
        return timeRanges;
    }

    @Test
    void shouldReturnUserBookingTimeRange() {
        // given
        UUID bookingId = UUID.randomUUID();
        Stream.of(
                booking(0, 45),
                booking(bookingId, aUserId(), 60, 120)
        ).forEach(this::persistBooking);

        // when
        GetUserBookingQuery query = new GetUserBookingQuery(aBookingPartitionKey(), aUserId(), bookingId);
        TimeRange result = service.getUserBookingTimeRange(query);

        // then
        TimeRange expected = TimeRange.Factory.create(60, 120);
        assertEquals(expected, result);
    }

    @Test
    void shouldThrowUserBookingNotFoundException() {
        // given
        Stream.of(
                booking(numberToUUID(1), aUserId(), 0, 45),
                booking(numberToUUID(2), aUserId(), 60, 120)
        ).forEach(this::persistBooking);

        // expect
        UUID bookingId = numberToUUID(3);
        GetUserBookingQuery query = new GetUserBookingQuery(aBookingPartitionKey(), aUserId(), bookingId);

        assertThrows(UserBookingNotFoundException.class, () -> service.getUserBookingTimeRange(query));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetUserBookingsTestCases")
    void getUserBookingsTest(
            String name,
            GetUserBookingsQuery query,
            List<UserBooking> expected
    ) {
        // given
        bookings().forEach(this::persistBooking);

        // when
        List<UserBooking> result = service.getUserBookings(query);

        // then
        assertEquals(expected, result);
    }

    private static Stream<Arguments> provideGetUserBookingsTestCases() {
        return Stream.of(
                Arguments.of(
                        "Should get first two bookings for user a",
                        new GetFirstUserBookingsQuery(aUserId(), 2),
                        List.of(
                                userBooking(numberToUUID(1), 0, 60),
                                userBooking(numberToUUID(3), 120, 250)
                        )
                ),
                Arguments.of(
                        "Should get two bookings for user a after the first",
                        getNextUserBookingQuery(aUserId(), numberToUUID(1), 2),
                        List.of(
                                userBooking(numberToUUID(3), 120, 250),
                                userBooking(numberToUUID(5), 400, 500)
                        )
                ),
                Arguments.of(
                        "Should get third bookings for user b after the first two",
                        getNextUserBookingQuery(bUserId(), numberToUUID(4), 2),
                        List.of(
                                userBooking(numberToUUID(6), 525, 600)
                        )
                ),
                Arguments.of(
                        "Should get no bookings",
                        getNextUserBookingQuery(aUserId(), numberToUUID(5), 2),
                        List.of(
                        )
                ),
                Arguments.of(
                        "Should get all bookings of user a",
                        new GetFirstUserBookingsQuery(aUserId(), 4),
                        List.of(
                                userBooking(numberToUUID(1), 0, 60),
                                userBooking(numberToUUID(3), 120, 250),
                                userBooking(numberToUUID(5), 400, 500)
                        )
                )
        );
    }

    private static List<Booking> bookings() {
        return List.of(
                booking(numberToUUID(1), aUserId(), 0, 60),
                booking(numberToUUID(2), bUserId(), 75, 105),
                booking(numberToUUID(3), aUserId(), 120, 250),
                booking(numberToUUID(4), bUserId(), 300, 375),
                booking(numberToUUID(5), aUserId(), 400, 500),
                booking(numberToUUID(6), bUserId(), 525, 600)
        );
    }

    public static GetNextUserBookingsQuery getNextUserBookingQuery(UUID userId, UUID bookingId, int limit) {
        BookingPartitionKey key = aBookingPartitionKey();
        return new GetNextUserBookingsQuery(
                userId,
                key.serviceId(),
                key.date(),
                bookingId,
                limit
        );
    }

    private static UserBooking userBooking(UUID bookingId, int start, int end) {
        BookingPartitionKey key = aBookingPartitionKey();
        return new UserBooking(
                key.serviceId(),
                key.date(),
                bookingId,
                start,
                end
        );
    }

    private static Booking booking(int start, int end) {
        UUID bookingId = UUID.randomUUID();
        UUID userId = aUserId();
        return booking(bookingId, userId, start, end);
    }

    private static Booking booking(UUID bookingId, UUID userId, int start, int end) {
        BookingPartitionKey key = aBookingPartitionKey();
        return newBooking(key.serviceId(), key.date(), bookingId, userId, start, end);
    }

    private static GetAvailableTimeRangesQuery getAvailableTimeRangesQuery(int duration) {
        BookingPartitionKey key = aBookingPartitionKey();
        return GetAvailableTimeRangesQuery.Factory.create(key.date(), key.serviceId(), duration);
    }

    private static BookingPartitionKey aBookingPartitionKey() {
        return BookingPartitionKey.Factory.create(numberToLocalDate(1), numberToUUID(1));
    }

    private static Booking newBooking(UUID serviceId, LocalDate date, UUID bookingId, UUID userId, int start, int end) {
        BookingByServiceAndDate bookingByServiceAndDate = BookingByServiceAndDate.builder()
                .serviceId(serviceId)
                .date(date)
                .bookingId(bookingId)
                .userId(userId)
                .start(start)
                .end(end)
                .build();

        BookingByUser bookingByUser = BookingByUser.builder()
                .userId(userId)
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

    private static UUID aUserId() {
        return numberToUUID(1);
    }

    private static UUID bUserId() {
        return numberToUUID(2);
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