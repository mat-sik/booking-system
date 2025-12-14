package com.github.matsik.query.booking.application;

import com.datastax.oss.driver.api.core.CqlSession;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.query.booking.CassandraAdapterConfig;
import com.github.matsik.query.booking.CassandraAdapterUtils.Booking;
import com.github.matsik.query.booking.TestDataGenerator;
import com.github.matsik.query.booking.application.domin.GetUserBookingsService;
import com.github.matsik.query.booking.application.domin.UserBooking;
import com.github.matsik.query.booking.application.port.in.GetFirstUserBookingsQuery;
import com.github.matsik.query.booking.application.port.in.GetNextUserBookingsQuery;
import com.github.matsik.query.booking.application.port.in.GetUserBookingsQuery;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.matsik.query.booking.CassandraAdapterUtils.aBookingPartitionKey;
import static com.github.matsik.query.booking.CassandraAdapterUtils.aUserId;
import static com.github.matsik.query.booking.CassandraAdapterUtils.bUserId;
import static com.github.matsik.query.booking.CassandraAdapterUtils.booking;
import static com.github.matsik.query.booking.CassandraAdapterUtils.userBooking;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {
        CassandraAdapterConfig.class,
        GetUserBookingsService.class,
})
class GetUserBookingsServiceTest extends CassandraBookingUseCaseTestBase {

    @Autowired
    private GetUserBookingsService getUserBookingsService;

    @Autowired
    public GetUserBookingsServiceTest(CqlSession cqlSession) {
        super(cqlSession);
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
        List<UserBooking> result = getUserBookingsService.getUserBookings(query);

        // then
        assertEquals(expected, result);
    }

    private static Stream<Arguments> provideGetUserBookingsTestCases() {
        return Stream.of(
                Arguments.of(
                        "Should get first two bookings for user a",
                        new GetFirstUserBookingsQuery(aUserId(), 2),
                        List.of(
                                userBooking(TestDataGenerator.numberToUUID(1), 0, 60),
                                userBooking(TestDataGenerator.numberToUUID(3), 120, 250)
                        )
                ),
                Arguments.of(
                        "Should get two bookings for user a after the first",
                        getNextUserBookingQuery(aUserId(), TestDataGenerator.numberToUUID(1), 2),
                        List.of(
                                userBooking(TestDataGenerator.numberToUUID(3), 120, 250),
                                userBooking(TestDataGenerator.numberToUUID(5), 400, 500)
                        )
                ),
                Arguments.of(
                        "Should get third bookings for user b after the first two",
                        getNextUserBookingQuery(bUserId(), TestDataGenerator.numberToUUID(4), 2),
                        List.of(
                                userBooking(TestDataGenerator.numberToUUID(6), 525, 600)
                        )
                ),
                Arguments.of(
                        "Should get no bookings",
                        getNextUserBookingQuery(aUserId(), TestDataGenerator.numberToUUID(5), 2),
                        List.of(
                        )
                ),
                Arguments.of(
                        "Should get all bookings of user a",
                        new GetFirstUserBookingsQuery(aUserId(), 4),
                        List.of(
                                userBooking(TestDataGenerator.numberToUUID(1), 0, 60),
                                userBooking(TestDataGenerator.numberToUUID(3), 120, 250),
                                userBooking(TestDataGenerator.numberToUUID(5), 400, 500)
                        )
                )
        );
    }

    private static List<Booking> bookings() {
        return List.of(
                booking(TestDataGenerator.numberToUUID(1), aUserId(), 0, 60),
                booking(TestDataGenerator.numberToUUID(2), bUserId(), 75, 105),
                booking(TestDataGenerator.numberToUUID(3), aUserId(), 120, 250),
                booking(TestDataGenerator.numberToUUID(4), bUserId(), 300, 375),
                booking(TestDataGenerator.numberToUUID(5), aUserId(), 400, 500),
                booking(TestDataGenerator.numberToUUID(6), bUserId(), 525, 600)
        );
    }

    private static GetNextUserBookingsQuery getNextUserBookingQuery(UUID userId, UUID bookingId, int limit) {
        BookingPartitionKey key = aBookingPartitionKey();
        return new GetNextUserBookingsQuery(
                userId,
                key.serviceId(),
                key.date(),
                bookingId,
                limit
        );
    }
}