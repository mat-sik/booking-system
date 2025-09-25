package com.github.matsik.command.booking.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
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
            boolean created,
            List<Booking> preTestState,
            CreateBookingCommand command
    ) {
        // given
        preTestState.forEach(bookingRepository::save);

        // when
        Optional<Booking> booking = bookingService.createBooking(command);

        // then
        if (created) {
            assertTrue(booking.isPresent());
            assertEquals(command.start(), booking.get().start());
            assertEquals(command.end(), booking.get().end());
            assertEquals(command.userId(), booking.get().userId());

            Optional<Booking> persistedBooking = findBooking(command.bookingPartitionKey(), booking.get().bookingId());

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

    public Optional<Booking> findBooking(BookingPartitionKey key, UUID bookingId) {
        List<Booking> bookings = findBookingsByStartTimeRange(key.serviceId(), key.date(), bookingId);
        if (bookings.size() > 1) {
            throw new IllegalStateException("Multiple bookings found");
        }
        return bookings.size() == 1 ? Optional.of(bookings.getFirst()) : Optional.empty();
    }

    private List<Booking> findBookingsByStartTimeRange(
            UUID serviceId,
            LocalDate date,
            UUID bookingId
    ) {
        String query = """
                SELECT service_id, date, booking_id, user_id, start, end
                FROM booking_system.bookings
                WHERE service_id = ? AND date = ? AND booking_id = ?
                """;

        PreparedStatement prepared = cqlSession.prepare(query);
        BoundStatement bound = prepared.bind(serviceId, date, bookingId);

        ResultSet resultSet = cqlSession.execute(bound);

        return resultSet.map(row ->
                Booking.builder()
                        .serviceId(row.getUuid("service_id"))
                        .date(row.getLocalDate("date"))
                        .bookingId(row.getUuid("booking_id"))
                        .userId(row.getUuid("user_id"))
                        .start(row.getInt("start"))
                        .end(row.getInt("end"))
                        .build()
        ).all();
    }

    private void clearBookingsTable() {
        cqlSession.execute("TRUNCATE booking_system.bookings");
    }

    private static CreateBookingCommand conflictingCreateBookingCommand(int start, int end) {
        return CreateBookingCommand.builder()
                .bookingPartitionKey(BookingPartitionKey.Factory.create(numberToLocalDate(1), numberToUUID(1)))
                .userId(numberToUUID(1))
                .start(start)
                .end(end)
                .build();
    }

    private static CreateBookingCommand nonConflictingOnDateCreateBookingCommand(int start, int end) {
        return CreateBookingCommand.builder()
                .bookingPartitionKey(BookingPartitionKey.Factory.create(numberToLocalDate(3), numberToUUID(1)))
                .userId(numberToUUID(1))
                .start(start)
                .end(end)
                .build();
    }

    private static CreateBookingCommand nonConflictingOnServiceCreateBookingCommand(int start, int end) {
        return CreateBookingCommand.builder()
                .bookingPartitionKey(BookingPartitionKey.Factory.create(numberToLocalDate(1), numberToUUID(2)))
                .userId(numberToUUID(1))
                .start(start)
                .end(end)
                .build();
    }

    private static Booking conflictingBooking(int start, int end) {
        return newBooking(
                1,
                start,
                end
        );
    }

    private static Booking nonConflictingBooking(int start, int end) {
        return newBooking(
                2,
                start,
                end
        );
    }

    private static Booking newBooking(int dateId, int start, int end) {
        return Booking.builder()
                .serviceId(numberToUUID(1))
                .date(numberToLocalDate(dateId))
                .bookingId(UUID.randomUUID())
                .userId(numberToUUID(1))
                .start(start)
                .end(end)
                .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDeleteBookingTestCases")
    void deleteBooking(
            String name,
            List<Booking> preTestState,
            DeleteBookingCommand command
    ) {
        // given
        preTestState.forEach(bookingRepository::save);

        // when
        bookingService.deleteBooking(command);

        // then
    }

    private static Stream<Arguments> provideDeleteBookingTestCases() {
        return Stream.of(
        );
    }

    public static UUID numberToUUID(long number) {
        String uuidString = String.format("%08d-0000-0000-0000-000000000000", number);
        return UUID.fromString(uuidString);
    }

    public static LocalDate numberToLocalDate(int number) {
        return LocalDate.of(2025, 9, number);
    }
}