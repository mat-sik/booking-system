package com.github.matsik.command.application.domain;

import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.cassandra.entity.BookingByUser;
import com.github.matsik.command.adapter.out.cassandra.BookingCache;
import com.github.matsik.command.adapter.out.cassandra.BookingPersistenceCachingAdapter;
import com.github.matsik.command.application.port.in.CreateBookingCommand;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.matsik.command.CassandraAdapterUtils.Booking;
import static com.github.matsik.command.CassandraAdapterUtils.conflictingBooking;
import static com.github.matsik.command.CassandraAdapterUtils.conflictingPartitionKey;
import static com.github.matsik.command.CassandraAdapterUtils.nonConflictingBooking;
import static com.github.matsik.command.CassandraAdapterUtils.nonConflictingOnDatePartitionKey;
import static com.github.matsik.command.CassandraAdapterUtils.nonConflictingOnServicePartitionKey;
import static com.github.matsik.command.CassandraAdapterUtils.userId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateBookingServiceTest extends CassandraBookingUseCaseTestBase {

    private CreateBookingService createBookingService;

    @BeforeEach
    void setUp() {
        BookingCache bookingCache = new BookingCache(bookingPersistenceService, new HashMap<>());
        BookingPersistenceCachingAdapter bookingPersistenceCachingAdapter = new BookingPersistenceCachingAdapter(bookingPersistenceService, bookingCache);
        createBookingService = new CreateBookingService(bookingPersistenceCachingAdapter);
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
        Optional<UUID> bookingId = createBookingService.createBooking(command);

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

    private static CreateBookingCommand createBookingCommand(BookingPartitionKey key, int start, int end) {
        return CreateBookingCommand.builder()
                .bookingPartitionKey(key)
                .userId(userId())
                .timeRange(TimeRange.of(start, end))
                .build();
    }

}