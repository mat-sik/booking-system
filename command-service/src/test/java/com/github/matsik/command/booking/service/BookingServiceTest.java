package com.github.matsik.command.booking.service;

import com.github.matsik.cassandra.model.BookingPartitionKey;
import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.repository.BookingRepository;
import com.github.matsik.command.config.cassandra.CassandraClientConfiguration;
import com.github.matsik.command.config.cassandra.CassandraClientProperties;
import com.github.matsik.command.migration.CassandraMigrationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootTest(classes = {
        BookingServiceTest.TestCassandraConfig.class,
        CassandraMigrationService.class,
        CassandraClientConfiguration.class,
        BookingService.class
})
@Testcontainers
class BookingServiceTest {

    @Container
    private static final CassandraContainer CASSANDRA_CONTAINER = new CassandraContainer("cassandra:5.0.5");

    @Autowired
    private BookingService bookingService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("cassandra.contactPoints", () -> String.format("%s:%d", CASSANDRA_CONTAINER.getHost(), CASSANDRA_CONTAINER.getFirstMappedPort()));
        registry.add("cassandra.keyspaceName", () -> "booking_system");
        registry.add("cassandra.datacenter", CASSANDRA_CONTAINER::getLocalDatacenter);
    }

    @Configuration
    @EnableConfigurationProperties(CassandraClientProperties.class)
    @EnableCassandraRepositories(basePackageClasses = BookingRepository.class)
    public static class TestCassandraConfig {
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCreateBookingTestCases")
    void createBooking(
            String name,
            CreateBookingCommand command
    ) {
        // given

        // when
        bookingService.createBooking(command);

        // then
    }

    private static Stream<Arguments> provideCreateBookingTestCases() {
        return Stream.of(
                Arguments.of(
                        "Create a new booking",
                        new CreateBookingCommand(
                                BookingPartitionKey.Factory.create(LocalDate.of(2025, 9, 18), numberToUUID(1)),
                                numberToUUID(1),
                                0,
                                30
                        )
                )
        );
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
}