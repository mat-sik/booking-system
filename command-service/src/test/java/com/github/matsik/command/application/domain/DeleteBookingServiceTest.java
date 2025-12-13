package com.github.matsik.command.application.domain;

import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.command.adapter.out.cassandra.BookingCache;
import com.github.matsik.command.adapter.out.cassandra.BookingPersistenceCachingAdapter;
import com.github.matsik.command.application.port.in.DeleteBookingCommand;
import com.github.matsik.dto.BookingPartitionKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.matsik.command.application.domain.CassandraAdapterUtils.Booking;
import static com.github.matsik.command.application.domain.CassandraAdapterUtils.conflictingBooking;
import static com.github.matsik.command.application.domain.CassandraAdapterUtils.conflictingPartitionKey;
import static com.github.matsik.command.application.domain.CassandraAdapterUtils.nonConflictingOnDatePartitionKey;
import static com.github.matsik.command.application.domain.CassandraAdapterUtils.nonConflictingOnServicePartitionKey;
import static com.github.matsik.command.application.domain.CassandraAdapterUtils.nonExistingBookingId;
import static com.github.matsik.command.application.domain.CassandraAdapterUtils.nonExistingUserId;
import static com.github.matsik.command.application.domain.CassandraAdapterUtils.userId;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteBookingServiceTest extends CassandraBookingUseCaseTestBase {

    private DeleteBookingService deleteBookingService;

    @BeforeEach
    void setUp() {
        BookingCache bookingCache = new BookingCache(bookingPersistenceService, new HashMap<>());
        BookingPersistenceCachingAdapter bookingPersistenceCachingAdapter = new BookingPersistenceCachingAdapter(bookingPersistenceService, bookingCache);
        deleteBookingService = new DeleteBookingService(bookingPersistenceCachingAdapter, recordCounter, recordHistogram);
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

        UUID bookingId = toDeleteBooking.bookingByServiceAndDate().bookingId();
        DeleteBookingCommand command = commandFunc.apply(bookingId);

        // when
        deleteBookingService.deleteBooking(command);

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
}