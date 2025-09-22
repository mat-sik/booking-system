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
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCreateBookingTestCases")
    void createBooking(
            String name,
            List<Booking> preTestState,
            CreateBookingCommand command
    ) {
        // given
        preTestState.forEach(bookingRepository::save);

        // when
        bookingService.createBooking(command);

        // then
        Booking persistedBooking = findConflictingBookings(command.start(), command.end());
        assertEquals(persistedBooking.userId(), command.userId());
    }

    private static Stream<Arguments> provideCreateBookingTestCases() {
        return Stream.of(
                Arguments.of(
                        "Create a new booking",
                        List.of(
                                conflictingBooking(0, 10),
                                conflictingBooking(20, 30),
                                nonConflictingBooking(0, 30)
                        ),
                        new CreateBookingCommand(
                                BookingPartitionKey.Factory.create(numberToLocalDate(1), numberToUUID(1)),
                                numberToUUID(1),
                                10,
                                20
                        )
                )
        );
    }

    public Booking findConflictingBookings(int start, int end) {
        List<Booking> bookings = findBookingsByStartTimeRange(numberToUUID(1), numberToLocalDate(1), start, end);
        if (bookings.size() > 1) {
            throw new IllegalStateException("Multiple bookings found");
        }
        return bookings.getFirst();
    }

    private List<Booking> findBookingsByStartTimeRange(
            UUID serviceId,
            LocalDate date,
            int startTimeFrom,
            int startTimeTo
    ) {
        String query = """
                SELECT service_id, date, end, start, booking_id, user_id
                FROM booking_system.bookings
                WHERE service_id = ? AND date = ? AND start = ? AND end = ?
                ALLOW FILTERING
                """;

        PreparedStatement prepared = cqlSession.prepare(query);
        BoundStatement bound = prepared.bind(serviceId, date, startTimeFrom, startTimeTo);

        ResultSet resultSet = cqlSession.execute(bound);

        return resultSet.map(row ->
                Booking.builder()
                        .serviceId(row.getUuid("service_id"))
                        .date(row.getLocalDate("date"))
                        .end(row.getInt("end"))
                        .start(row.getInt("start"))
                        .bookingId(row.getUuid("booking_id"))
                        .userId(row.getUuid("user_id"))
                        .build()
        ).all();
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
            Runnable createPreExistingBookings,
            DeleteBookingCommand command,
            boolean isSuccessful,
            boolean matchedButNoBooking
    ) {
        // given
        createPreExistingBookings.run();

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