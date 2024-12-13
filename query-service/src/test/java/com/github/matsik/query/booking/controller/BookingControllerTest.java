package com.github.matsik.query.booking.controller;

import com.github.matsik.query.ControllerAdvice;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.service.BookingService;
import com.github.matsik.query.booking.service.TimeRange;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class BookingControllerTest {

    private static final BookingService SERVICE = mock(BookingService.class);
    private static final BookingController CONTROLLER = new BookingController(SERVICE);
    private static final ControllerAdvice CONTROLLER_ADVICE = new ControllerAdvice();
    private static final MockMvc MOCK_MVC = MockMvcBuilders
            .standaloneSetup(CONTROLLER)
            .setControllerAdvice(CONTROLLER_ADVICE)
            .build();

    private static Stream<Arguments> provideGetAvailableTimeRangesTestCases() {
        return Stream.of(
                Arguments.of(
                        "OK response.",
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        new ObjectId("000000000000000000000000").toHexString(),
                        String.valueOf(75),
                        List.of(
                                new TimeRange(900, 990),
                                new TimeRange(990, 1080)
                        ),
                        (MvcAssertExpectation<List<TimeRange>>) (resultActions, availableTimeRanges) -> {
                            resultActions
                                    .andExpect(status().isOk())
                                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                    .andExpect(jsonPath("$").isArray())
                                    .andExpect(jsonPath("$.length()").value(availableTimeRanges.size()));

                            for (int i = 0; i < availableTimeRanges.size(); i++) {
                                resultActions
                                        .andExpect(jsonPath("$[%d].start", i).value(availableTimeRanges.get(i).start()))
                                        .andExpect(jsonPath("$[%d].end", i).value(availableTimeRanges.get(i).end()))
                                        .andExpect(jsonPath(String.format("$[%d]", i), aMapWithSize(2)));
                            }
                        },
                        (Consumer<GetAvailableTimeRangesQuery>) (query) -> {
                            then(SERVICE).should().getAvailableTimeRanges(query);
                            then(SERVICE).shouldHaveNoMoreInteractions();
                        }
                ),
                Arguments.of(
                        "Invalid service duration number format.",
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        new ObjectId("000000000000000000000000").toHexString(),
                        "invalid format",
                        List.of(
                                new TimeRange(900, 990),
                                new TimeRange(990, 1080)
                        ),
                        (MvcAssertExpectation<List<TimeRange>>) (resultActions, availableTimeRanges) -> {
                            resultActions
                                    .andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                                    .andExpect(jsonPath("$", aMapWithSize(5)));
                        },
                        (Consumer<GetAvailableTimeRangesQuery>) (query) -> then(SERVICE).shouldHaveNoMoreInteractions()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetAvailableTimeRangesTestCases")
    void getAvailableTimeRanges(
            String name,
            String date,
            String serviceId,
            String serviceDuration,
            List<TimeRange> availableTimeRanges,
            MvcAssertExpectation<List<TimeRange>> mvcAssertExpectation,
            Consumer<GetAvailableTimeRangesQuery> assertMock
    ) throws Exception {
        // given
        GetAvailableTimeRangesQuery query = getGetAvailableTimeRangesQueryOrDefault(date, serviceId, serviceDuration);
        when(SERVICE.getAvailableTimeRanges(eq(query)))
                .thenReturn(availableTimeRanges);

        // when
        ResultActions resultActions = MOCK_MVC.perform(get("/booking/available")
                .param("date", date)
                .param("serviceId", serviceId)
                .param("serviceDuration", serviceDuration)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        mvcAssertExpectation.expect(resultActions, availableTimeRanges);

        assertMock.accept(query);
    }

    @FunctionalInterface
    private interface MvcAssertExpectation<T> {
        void expect(ResultActions resultActions, T expectation) throws Exception;
    }

    private static GetAvailableTimeRangesQuery getGetAvailableTimeRangesQueryOrDefault(
            String dateString,
            String serviceIdString,
            String serviceDurationString
    ) {
        try {
            LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
            ObjectId serviceId = new ObjectId(serviceIdString);
            int serviceDuration = Integer.parseInt(serviceDurationString);

            return GetAvailableTimeRangesQuery.Factory.create(date, serviceId, serviceDuration);
        } catch (Exception ex) {
            return GetAvailableTimeRangesQuery.Factory.create(
                    LocalDate.of(2000, 1, 1),
                    new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa"),
                    60
            );
        }
    }

    @Test
    void getUserBooking() {
    }

    @Test
    void getBookings() {
    }
}