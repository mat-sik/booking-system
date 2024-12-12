package com.github.matsik.query.booking.service;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingTimeRangesQuery;
import com.github.matsik.query.booking.repository.BookingRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.params.ParameterizedTest;
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

    private static Stream<GetAvailableTimeRangesTestCase> provideGetAvailableTimeRangesTestScenarios() {
        return Stream.of(
                new GetAvailableTimeRangesTestCase(
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
                new GetAvailableTimeRangesTestCase(
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
                new GetAvailableTimeRangesTestCase(
                        "No available time ranges.",
                        100,
                        List.of(
                                new TimeRange(60, 1400)
                        ),
                        List.of()
                ),
                new GetAvailableTimeRangesTestCase(
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

    @ParameterizedTest
    @MethodSource("provideGetAvailableTimeRangesTestScenarios")
    void getAvailableTimeRanges(GetAvailableTimeRangesTestCase testCase) {
        // given
        given(repository.getBookingTimeRanges(testCase.getBookingTimeRangesQuery)).willReturn(testCase.unavailableTimeRanges);

        // when
        List<TimeRange> result = service.getAvailableTimeRanges(testCase.getAvailableTimeRangesQuery);

        // then
        then(repository).should().getBookingTimeRanges(testCase.getBookingTimeRangesQuery);
        then(repository).shouldHaveNoMoreInteractions();

        assertThat(result).isEqualTo(testCase.expected);
    }

    private static class GetAvailableTimeRangesTestCase {
        private static final ObjectId SERVICE_ID = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        private static final LocalDate DATE = LocalDate.of(2024, 12, 3);

        private final String description;
        private final GetBookingTimeRangesQuery getBookingTimeRangesQuery;
        private final GetAvailableTimeRangesQuery getAvailableTimeRangesQuery;
        private final List<TimeRange> unavailableTimeRanges;
        private final List<TimeRange> expected;

        private GetAvailableTimeRangesTestCase(String description, int serviceDuration, List<TimeRange> unavailableTimeRanges, List<TimeRange> expected) {
            this.description = description;

            var serviceBookingIdentifier = ServiceBookingIdentifier.Factory.create(DATE, SERVICE_ID);

            this.getBookingTimeRangesQuery = new GetBookingTimeRangesQuery(serviceBookingIdentifier);
            this.getAvailableTimeRangesQuery = new GetAvailableTimeRangesQuery(getBookingTimeRangesQuery, serviceDuration);

            this.unavailableTimeRanges = unavailableTimeRanges;

            this.expected = expected;
        }

        @Override
        public String toString() {
            return "Scenario: " + description;
        }
    }
}