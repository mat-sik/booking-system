package com.github.matsik.command.booking.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.github.matsik.cassandra.model.Booking;
import com.github.matsik.cassandra.model.BookingPartitionKey;
import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.repository.BookingRepository;
import com.github.matsik.command.config.cassandra.client.CassandraClientConfiguration;
import com.github.matsik.command.config.cassandra.client.CassandraClientProperties;
import com.github.matsik.command.config.cassandra.mapper.booking.BookingMapperConfiguration;
import com.github.matsik.command.migration.CassandraMigrationService;
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
    private BookingRepository bookingRepository;

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
        preTestState.forEach(bookingRepository::save);

        // when
        Optional<Booking> booking = bookingService.createBooking(command);

        // then
        if (shouldCreate) {
            assertTrue(booking.isPresent());
            assertEquals(command.start(), booking.get().start());
            assertEquals(command.end(), booking.get().end());
            assertEquals(command.userId(), booking.get().userId());

            Optional<Booking> persistedBooking = findBookingByBookingId(command.bookingPartitionKey(), booking.get().bookingId());

            assertTrue(persistedBooking.isPresent());
            assertEquals(command.start(), persistedBooking.get().start());
            assertEquals(command.end(), persistedBooking.get().end());
            assertEquals(command.userId(), persistedBooking.get().userId());
        } else {
            assertTrue(booking.isEmpty());
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
                        conflictingCreateBookingCommand(10, 20)
                ),
                Arguments.of(
                        "Should fail to create a booking in the occupied time range",
                        false,
                        List.of(
                                conflictingBooking(0, 30)
                        ),
                        conflictingCreateBookingCommand(10, 20)
                ),
                Arguments.of(
                        "Should create a booking in the available time range, because of a different date",
                        true,
                        List.of(
                                conflictingBooking(0, 30)
                        ),
                        nonConflictingOnDateCreateBookingCommand(10, 20)
                ),
                Arguments.of(
                        "Should create a booking in the available time range, because of a different service",
                        true,
                        List.of(
                                conflictingBooking(0, 30)
                        ),
                        nonConflictingOnServiceCreateBookingCommand(10, 20)
                )
        );
    }

    private Optional<Booking> findBookingByBookingId(BookingPartitionKey key, UUID bookingId) {
        String query = """
                SELECT service_id, date, booking_id, user_id, start, end
                FROM booking_system.bookings
                WHERE service_id = ? AND date = ? AND booking_id = ?
                """;

        PreparedStatement prepared = cqlSession.prepare(query);
        BoundStatement bound = prepared.bind(key.serviceId(), key.date(), bookingId);

        ResultSet resultSet = cqlSession.execute(bound);

        return Optional.ofNullable(resultSet.map(this::mapFrom).one());
    }

    private void clearBookingsTable() {
        cqlSession.execute("TRUNCATE booking_system.bookings");
    }

    private static CreateBookingCommand conflictingCreateBookingCommand(int start, int end) {
        return CreateBookingCommand.builder()
                .bookingPartitionKey(conflictingPartitionKey())
                .userId(userId())
                .start(start)
                .end(end)
                .build();
    }

    private static CreateBookingCommand nonConflictingOnDateCreateBookingCommand(int start, int end) {
        return CreateBookingCommand.builder()
                .bookingPartitionKey(nonConflictingOnDatePartitionKey())
                .userId(userId())
                .start(start)
                .end(end)
                .build();
    }

    private static BookingPartitionKey nonConflictingOnDatePartitionKey() {
        return BookingPartitionKey.Factory.create(numberToLocalDate(3), numberToUUID(2));
    }

    private static CreateBookingCommand nonConflictingOnServiceCreateBookingCommand(int start, int end) {
        return CreateBookingCommand.builder()
                .bookingPartitionKey(nonConflictingOnServicePartitionKey())
                .userId(userId())
                .start(start)
                .end(end)
                .build();
    }

    private static BookingPartitionKey nonConflictingOnServicePartitionKey() {
        return nonConflictingPartitionKey();
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
        preTestState.forEach(bookingRepository::save);
        Booking toDeleteBooking = preTestState.getFirst();

        UUID bookingId = shouldDelete ? UUID.randomUUID() : toDeleteBooking.bookingId();
        DeleteBookingCommand command = commandFunc.apply(bookingId);

        int start = shouldDelete ? -1 : toDeleteBooking.start();
        int end = shouldDelete ? -1 : toDeleteBooking.end();
        // when
        bookingService.deleteBooking(command);

        // then
        Optional<Booking> persistedBooking = findBookingByTimeRange(command.bookingPartitionKey(), start, end);
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
                        (Function<UUID, DeleteBookingCommand>) (bookingId) -> new DeleteBookingCommand(
                                conflictingPartitionKey(),
                                bookingId
                        )
                ),
                Arguments.of(
                        "Should do nothing if booking doesn't exist",
                        false,
                        List.of(
                                conflictingBooking(0, 30)
                        ),
                        (Function<UUID, DeleteBookingCommand>) (_) -> new DeleteBookingCommand(
                                conflictingPartitionKey(),
                                nonExistingBookingId()
                        )
                )
        );
    }

    private Optional<Booking> findBookingByTimeRange(BookingPartitionKey key, int start, int end) {
        String query = """
                SELECT service_id, date, booking_id, user_id, start, end
                FROM booking_system.bookings
                WHERE service_id = ? AND date = ? AND start = ? AND end = ?
                ALLOW FILTERING
                """;

        PreparedStatement prepared = cqlSession.prepare(query);
        BoundStatement bound = prepared.bind(key.serviceId(), key.date(), start, end);

        ResultSet resultSet = cqlSession.execute(bound);

        List<Booking> bookings = resultSet.map(this::mapFrom).all();

        if (bookings.size() > 1) {
            throw new IllegalStateException("Multiple bookings found, incorrect test definition");
        }
        return bookings.isEmpty() ? Optional.empty() : Optional.of(bookings.getFirst());
    }

    private Booking mapFrom(Row row) {
        return Booking.builder()
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
                key.date(),
                start,
                end
        );
    }

    private static BookingPartitionKey conflictingPartitionKey() {
        return BookingPartitionKey.Factory.create(numberToLocalDate(1), numberToUUID(1));
    }

    private static Booking nonConflictingBooking(int start, int end) {
        BookingPartitionKey key = nonConflictingPartitionKey();
        return newBooking(
                key.serviceId(),
                key.date(),
                start,
                end
        );
    }

    private static BookingPartitionKey nonConflictingPartitionKey() {
        return BookingPartitionKey.Factory.create(numberToLocalDate(1), numberToUUID(2));
    }

    private static Booking newBooking(UUID serviceId, LocalDate date, int start, int end) {
        return Booking.builder()
                .serviceId(serviceId)
                .date(date)
                .bookingId(UUID.randomUUID())
                .userId(userId())
                .start(start)
                .end(end)
                .build();
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