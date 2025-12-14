package com.github.matsik.query.booking.application;

import com.datastax.oss.driver.api.core.CqlSession;
import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.CassandraAdapterConfig;
import com.github.matsik.query.booking.TestDataGenerator;
import com.github.matsik.query.booking.application.domin.GetUserBookingService;
import com.github.matsik.query.booking.application.port.in.GetUserBookingQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.matsik.query.booking.CassandraAdapterUtils.aBookingPartitionKey;
import static com.github.matsik.query.booking.CassandraAdapterUtils.aUserId;
import static com.github.matsik.query.booking.CassandraAdapterUtils.booking;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = {
        CassandraAdapterConfig.class,
        GetUserBookingService.class,
})
class GetUserBookingServiceTest extends CassandraBookingUseCaseTestBase {

    @Autowired
    private GetUserBookingService getUserBookingService;

    @Autowired
    public GetUserBookingServiceTest(CqlSession cqlSession) {
        super(cqlSession);
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
        Optional<TimeRange> result = getUserBookingService.getUserBookingTimeRange(query);

        // then
        assertTrue(result.isPresent());

        TimeRange expected = TimeRange.of(60, 120);
        assertEquals(expected, result.get());
    }

    @Test
    void shouldThrowUserBookingNotFoundException() {
        // given
        Stream.of(
                booking(TestDataGenerator.numberToUUID(1), aUserId(), 0, 45),
                booking(TestDataGenerator.numberToUUID(2), aUserId(), 60, 120)
        ).forEach(this::persistBooking);

        // expect
        UUID bookingId = TestDataGenerator.numberToUUID(3);
        GetUserBookingQuery query = new GetUserBookingQuery(aBookingPartitionKey(), aUserId(), bookingId);

        Optional<TimeRange> result = getUserBookingService.getUserBookingTimeRange(query);
        assertTrue(result.isEmpty());
    }
}