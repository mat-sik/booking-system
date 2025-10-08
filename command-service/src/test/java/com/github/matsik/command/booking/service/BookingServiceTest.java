package com.github.matsik.command.booking.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchType;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.cassandra.entity.BookingByUser;
import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.config.cassandra.client.CassandraClientConfiguration;
import com.github.matsik.command.config.cassandra.client.CassandraClientProperties;
import com.github.matsik.command.config.cassandra.mapper.booking.BookingMapperConfiguration;
import com.github.matsik.command.migration.CassandraMigrationService;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
        BookingServiceTest.TestCassandraConfig.class,
        CassandraMigrationService.class,
        CassandraClientConfiguration.class,
        BookingMapperConfiguration.class,
        BookingService.class
})
@Testcontainers
class BookingServiceTest {

    @Container
    private static final CassandraContainer CASSANDRA_CONTAINER = new CassandraContainer("cassandra:5.0.5");

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CqlSession cqlSession;

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

    @AfterEach
    void tearDown() {
        clearBookingsTable();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCreateBookingTestCases")
    void createBooking(
            String name,
            boolean shouldCreate,
            List<Booking> preTestState,
            CreateBookingCommand command
    ) {
        // given
        preTestState.forEach(this::persistBooking);

        // when
        Optional<UUID> bookingId = bookingService.createBooking(command);

        // then
        if (shouldCreate) {
            assertTrue(bookingId.isPresent());
            Optional<BookingByServiceAndDate> persistedBookingByServiceAndDate = findBookingByServiceAndDate(command.bookingPartitionKey(), bookingId.get());

            assertTrue(persistedBookingByServiceAndDate.isPresent());
            assertEquals(command.userId(), persistedBookingByServiceAndDate.get().userId());
            assertEquals(command.timeRange().start().minuteOfDay(), persistedBookingByServiceAndDate.get().start());
            assertEquals(command.timeRange().end().minuteOfDay(), persistedBookingByServiceAndDate.get().end());

            Optional<BookingByUser> persistedBookingByUser = findBookingByUser(command.userId(), command.bookingPartitionKey(), bookingId.get());

            assertTrue(persistedBookingByUser.isPresent());
            assertEquals(command.userId(), persistedBookingByUser.get().userId());
            assertEquals(command.timeRange().start().minuteOfDay(), persistedBookingByUser.get().start());
            assertEquals(command.timeRange().end().minuteOfDay(), persistedBookingByUser.get().end());
        } else {
            assertTrue(bookingId.isEmpty());
        }
    }

    private static Stream<Arguments> provideCreateBookingTestCases() {
        return Stream.of(
                Arguments.of(
                        "Should create a booking in the available time range",
                        true,
                        List.of(
                                conflictingBooking(0, 10),
                                conflictingBooking(20, 30),
                                nonConflictingBooking(0, 30)
                        ),
                        createBookingCommand(conflictingPartitionKey(), 10, 20)
                ),
                Arguments.of(
                        "Should fail to create a booking in the occupied time range",
                        false,
                        List.of(
                                conflictingBooking(0, 30)
                        ),
                        createBookingCommand(conflictingPartitionKey(), 10, 20)
                ),
                Arguments.of(
                        "Should create a booking in the available time range, because of a different date",
                        true,
                        List.of(
                                conflictingBooking(0, 30)
                        ),
                        createBookingCommand(nonConflictingOnDatePartitionKey(), 10, 20)
                ),
                Arguments.of(
                        "Should create a booking in the available time range, because of a different service",
                        true,
                        List.of(
                                conflictingBooking(0, 30)
                        ),
                        createBookingCommand(nonConflictingOnServicePartitionKey(), 10, 20)
                )
        );
    }

    private Optional<BookingByServiceAndDate> findBookingByServiceAndDate(BookingPartitionKey key, UUID bookingId) {
        String query = """
                SELECT service_id, date, booking_id, user_id, start, end
                FROM booking_system.bookings_by_service_and_date
                WHERE service_id = ? AND date = ? AND booking_id = ?
                """;

        PreparedStatement prepared = cqlSession.prepare(query);
        BoundStatement bound = prepared.bind(key.serviceId(), key.date(), bookingId);

        ResultSet resultSet = cqlSession.execute(bound);

        return Optional.ofNullable(resultSet.map(this::bookingByServiceAndDate).one());
    }

    private Optional<BookingByUser> findBookingByUser(UUID userId, BookingPartitionKey key, UUID bookingId) {
        String query = """
                SELECT service_id, date, booking_id, user_id, start, end
                FROM booking_system.bookings_by_user
                WHERE user_id = ? AND service_id = ? AND date = ? AND booking_id = ?
                """;

        PreparedStatement prepared = cqlSession.prepare(query);
        BoundStatement bound = prepared.bind(userId, key.serviceId(), key.date(), bookingId);

        ResultSet resultSet = cqlSession.execute(bound);

        return Optional.ofNullable(resultSet.map(this::bookingByUser).one());
    }

    private void clearBookingsTable() {
        cqlSession.execute("TRUNCATE booking_system.bookings_by_service_and_date");
        cqlSession.execute("TRUNCATE booking_system.bookings_by_user");
    }

    private static CreateBookingCommand createBookingCommand(BookingPartitionKey key, int start, int end) {
        return CreateBookingCommand.builder()
                .bookingPartitionKey(key)
                .userId(userId())
                .timeRange(TimeRange.of(start, end))
                .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDeleteBookingTestCases")
    void deleteBooking(
            String name,
            boolean shouldDelete,
            List<Booking> preTestState,
            Function<UUID, DeleteBookingCommand> commandFunc
    ) {
        // given
        preTestState.forEach(this::persistBooking);
        Booking toDeleteBooking = preTestState.getFirst();

        UUID bookingId = toDeleteBooking.bookingByServiceAndDate.bookingId();
        DeleteBookingCommand command = commandFunc.apply(bookingId);

        // when
        bookingService.deleteBooking(command);

        // then
        Optional<BookingByServiceAndDate> persistedBooking = findBooking(toDeleteBooking);
        if (shouldDelete) {
            assertTrue(persistedBooking.isEmpty());
        } else {
            assertTrue(persistedBooking.isPresent());
        }
    }

    private static Stream<Arguments> provideDeleteBookingTestCases() {
        return Stream.of(
                Arguments.of(
                        "Should delete existing booking",
                        true,
                        List.of(
                                conflictingBooking(0, 30),
                                conflictingBooking(0, 60)
                        ),
                        (Function<UUID, DeleteBookingCommand>) (bookingId) -> deleteBookingCommand(
                                conflictingPartitionKey(),
                                bookingId,
                                userId()
                        )
                ),
                Arguments.of(
                        "Should do nothing if booking id doesn't match",
                        false,
                        List.of(
                                conflictingBooking(0, 30)
                        ),
                        (Function<UUID, DeleteBookingCommand>) (_) -> deleteBookingCommand(
                                conflictingPartitionKey(),
                                nonExistingBookingId(),
                                userId()
                        )
                ),
                Arguments.of(
                        "Should do nothing if partition key on service doesn't match",
                        false,
                        List.of(
                                conflictingBooking(0, 30)
                        ),
                        (Function<UUID, DeleteBookingCommand>) (bookingId) -> deleteBookingCommand(
                                nonConflictingOnServicePartitionKey(),
                                bookingId,
                                userId()
                        )
                ),
                Arguments.of(
                        "Should do nothing if partition key on date doesn't match",
                        false,
                        List.of(
                                conflictingBooking(0, 30)
                        ),
                        (Function<UUID, DeleteBookingCommand>) (bookingId) -> deleteBookingCommand(
                                nonConflictingOnDatePartitionKey(),
                                bookingId,
                                userId()
                        )
                ),
                Arguments.of(
                        "Should do nothing if user doesn't match",
                        false,
                        List.of(
                                conflictingBooking(0, 30)
                        ),
                        (Function<UUID, DeleteBookingCommand>) (bookingId) -> deleteBookingCommand(
                                conflictingPartitionKey(),
                                bookingId,
                                nonExistingUserId()
                        )

                )
        );
    }

    private static DeleteBookingCommand deleteBookingCommand(BookingPartitionKey key, UUID bookingId, UUID userId) {
        return new DeleteBookingCommand(
                key,
                bookingId,
                userId
        );
    }

    private static BookingPartitionKey nonConflictingOnServicePartitionKey() {
        return BookingPartitionKey.of(numberToUUID(2), numberToLocalDate(1));
    }

    private static BookingPartitionKey nonConflictingOnDatePartitionKey() {
        return BookingPartitionKey.of(numberToUUID(1), numberToLocalDate(2));
    }

    private Optional<BookingByServiceAndDate> findBooking(Booking booking) {
        BookingByServiceAndDate bookingByServiceAndDate = booking.bookingByServiceAndDate;

        UUID serviceId = bookingByServiceAndDate.serviceId();
        LocalDate date = bookingByServiceAndDate.date();
        int start = bookingByServiceAndDate.start();
        int end = bookingByServiceAndDate.end();

        return findBookingByTimeRange(serviceId, date, start, end);
    }

    private Optional<BookingByServiceAndDate> findBookingByTimeRange(UUID serviceId, LocalDate date, int start, int end) {
        String query = """
                SELECT service_id, date, booking_id, user_id, start, end
                FROM booking_system.bookings_by_service_and_date
                WHERE service_id = ? AND date = ? AND start = ? AND end = ?
                ALLOW FILTERING
                """;

        PreparedStatement prepared = cqlSession.prepare(query);
        BoundStatement bound = prepared.bind(serviceId, date, start, end);

        ResultSet resultSet = cqlSession.execute(bound);

        List<BookingByServiceAndDate> bookings = resultSet.map(this::bookingByServiceAndDate).all();

        if (bookings.size() > 1) {
            throw new IllegalStateException("Multiple bookings found, incorrect test definition");
        }
        return bookings.isEmpty() ? Optional.empty() : Optional.of(bookings.getFirst());
    }

    private BookingByServiceAndDate bookingByServiceAndDate(Row row) {
        return BookingByServiceAndDate.builder()
                .serviceId(row.getUuid("service_id"))
                .date(row.getLocalDate("date"))
                .bookingId(row.getUuid("booking_id"))
                .userId(row.getUuid("user_id"))
                .start(row.getInt("start"))
                .end(row.getInt("end"))
                .build();
    }

    private BookingByUser bookingByUser(Row row) {
        return BookingByUser.builder()
                .serviceId(row.getUuid("service_id"))
                .date(row.getLocalDate("date"))
                .bookingId(row.getUuid("booking_id"))
                .userId(row.getUuid("user_id"))
                .start(row.getInt("start"))
                .end(row.getInt("end"))
                .build();
    }

    private static UUID nonExistingBookingId() {
        return numberToUUID(10);
    }

    private static Booking conflictingBooking(int start, int end) {
        BookingPartitionKey key = conflictingPartitionKey();
        return newBooking(
                key.serviceId(),
                UUID.randomUUID(),
                key.date(),
                start,
                end
        );
    }

    private static BookingPartitionKey conflictingPartitionKey() {
        return BookingPartitionKey.of(numberToUUID(1), numberToLocalDate(1));
    }

    private static Booking nonConflictingBooking(int start, int end) {
        BookingPartitionKey key = nonConflictingPartitionKey();
        return newBooking(
                key.serviceId(),
                UUID.randomUUID(),
                key.date(),
                start,
                end
        );
    }

    private static BookingPartitionKey nonConflictingPartitionKey() {
        return BookingPartitionKey.of(numberToUUID(3), numberToLocalDate(3));
    }

    private static Booking newBooking(UUID serviceId, UUID bookingId, LocalDate date, int start, int end) {
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

    private static UUID nonExistingUserId() {
        return numberToUUID(2);
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
}