package com.github.matsik.query.application.domain;

import com.datastax.oss.driver.api.core.CqlSession;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.CassandraAdapterConfig;
import com.github.matsik.query.CassandraAdapterUtils.Booking;
import com.github.matsik.query.application.domin.AvailableTimeRangesCalculator;
import com.github.matsik.query.application.domin.GetAvailableTimeRangesService;
import com.github.matsik.query.application.port.in.GetAvailableTimeRangesQuery;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.github.matsik.query.CassandraAdapterUtils.aBookingPartitionKey;
import static com.github.matsik.query.CassandraAdapterUtils.booking;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {
        CassandraAdapterConfig.class,
        AvailableTimeRangesCalculator.class,
        GetAvailableTimeRangesService.class,
})
class GetAvailableTimeRangesServiceTest extends CassandraBookingUseCaseTestBase {

    @Autowired
    private GetAvailableTimeRangesService getAvailableTimeRangesService;

    @Autowired
    public GetAvailableTimeRangesServiceTest(CqlSession cqlSession) {
        super(cqlSession);
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
        List<TimeRange> result = getAvailableTimeRangesService.getAvailableTimeRanges(query);

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
                                TimeRange.of(675, 795)
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
                                TimeRange.of(135, 195),
                                TimeRange.of(150, 210),
                                TimeRange.of(165, 225),
                                TimeRange.of(180, 240),
                                TimeRange.of(195, 255),
                                TimeRange.of(675, 735),
                                TimeRange.of(690, 750)
                        )
                )
        );
    }

    private static List<TimeRange> timeRangesForEmptyDay(int serviceDuration) {
        List<TimeRange> timeRanges = new ArrayList<>();
        for (int start = 0; start <= 24 * 60 - serviceDuration; start += 15) {
            timeRanges.add(TimeRange.of(start, start + serviceDuration));
        }
        return timeRanges;
    }

    private static GetAvailableTimeRangesQuery getAvailableTimeRangesQuery(int duration) {
        BookingPartitionKey key = aBookingPartitionKey();
        return GetAvailableTimeRangesQuery.of(key.serviceId(), key.date(), duration);
    }
}