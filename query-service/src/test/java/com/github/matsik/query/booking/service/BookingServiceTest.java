package com.github.matsik.query.booking.service;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingTimeRangesQuery;
import com.github.matsik.query.booking.repository.BookingRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class BookingServiceTest {

    private final BookingRepository repository = Mockito.mock(BookingRepository.class);

    private final BookingService service = new BookingService(repository);

    private static Stream<Arguments> provideGetAvailableTimeRangesTestScenarios() {
        return Stream.of(
                getGetAvailableTimeRangesArguments(
                        "Many available time ranges.",
                        100,
                        List.of(
                                new TimeRange(600, 660),
                                new TimeRange(850, 900)
                        ),
                        List.of(
                                new TimeRange(0, 120),
                                new TimeRange(120, 240),
                                new TimeRange(240, 360),
                                new TimeRange(360, 480),
                                new TimeRange(480, 600),
                                new TimeRange(720, 840),
                                new TimeRange(960, 1080),
                                new TimeRange(1080, 1200),
                                new TimeRange(1200, 1320),
                                new TimeRange(1320, 1440)
                        )
                ),
                getGetAvailableTimeRangesArguments(
                        "No unavailable time ranges.",
                        100,
                        List.of(),
                        List.of(
                                new TimeRange(0, 120),
                                new TimeRange(120, 240),
                                new TimeRange(240, 360),
                                new TimeRange(360, 480),
                                new TimeRange(480, 600),
                                new TimeRange(600, 720),
                                new TimeRange(720, 840),
                                new TimeRange(840, 960),
                                new TimeRange(960, 1080),
                                new TimeRange(1080, 1200),
                                new TimeRange(1200, 1320),
                                new TimeRange(1320, 1440)
                        )
                ),
                getGetAvailableTimeRangesArguments(
                        "No available time ranges.",
                        100,
                        List.of(
                                new TimeRange(60, 1400)
                        ),
                        List.of()
                ),
                getGetAvailableTimeRangesArguments(
                        "Zero service duration.",
                        0,
                        List.of(
                                new TimeRange(0, 1200),
                                new TimeRange(1320, 1440)
                        ),
                        List.of(

                                new TimeRange(1200, 1230),
                                new TimeRange(1230, 1260),
                                new TimeRange(1260, 1290),
                                new TimeRange(1290, 1320)
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetAvailableTimeRangesTestScenarios")
    void getAvailableTimeRanges(
            String name,
            GetBookingTimeRangesQuery getBookingTimeRangesQuery,
            GetAvailableTimeRangesQuery getAvailableTimeRangesQuery,
            List<TimeRange> unavailableTimeRanges,
            List<TimeRange> expected
    ) {
        // given
        given(repository.getBookingTimeRanges(getBookingTimeRangesQuery)).willReturn(unavailableTimeRanges);

        // when
        List<TimeRange> result = service.getAvailableTimeRanges(getAvailableTimeRangesQuery);

        // then
        then(repository).should().getBookingTimeRanges(getBookingTimeRangesQuery);
        then(repository).shouldHaveNoMoreInteractions();

        assertThat(result).isEqualTo(expected);
    }

    private static Arguments getGetAvailableTimeRangesArguments(
            String name,
            int serviceDuration,
            List<TimeRange> unavailableTimeRanges,
            List<TimeRange> expected
    ) {
        ServiceBookingIdentifier identifier = getDefaultIdentifier();
        GetBookingTimeRangesQuery getBookingTimeRangesQuery = new GetBookingTimeRangesQuery(identifier);
        GetAvailableTimeRangesQuery getAvailableTimeRangesQuery = new GetAvailableTimeRangesQuery(getBookingTimeRangesQuery, serviceDuration);

        return Arguments.of(
                name,
                getBookingTimeRangesQuery,
                getAvailableTimeRangesQuery,
                unavailableTimeRanges,
                expected
        );
    }

    private static ServiceBookingIdentifier getDefaultIdentifier() {
        LocalDate date = LocalDate.of(2024, 12, 3);
        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        return ServiceBookingIdentifier.Factory.create(date, serviceId);
    }
}