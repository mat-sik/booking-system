package com.github.matsik.query.booking.controller;

import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetUserBookingQuery;
import com.github.matsik.query.booking.service.BookingService;
import com.github.matsik.query.booking.service.exception.UserBookingNotFoundException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService service;

    @ParameterizedTest(name = "{0}")
    @MethodSource("getAvailableTimeRangesArguments")
    void getAvailableTimeRanges(
            String name,
            String serviceId,
            String date,
            String serviceDuration,
            List<TimeRange> expectedTimeRanges,
            int expectedStatus,
            BiConsumer<ResultActions, List<TimeRange>> bodyChecker
    ) throws Exception {
        // given
        if (!expectedTimeRanges.isEmpty()) {
            GetAvailableTimeRangesQuery query = GetAvailableTimeRangesQuery.of(
                    UUID.fromString(serviceId),
                    LocalDate.parse(date),
                    Integer.parseInt(serviceDuration)
            );
            when(service.getAvailableTimeRanges(query)).thenReturn(expectedTimeRanges);
        }
        // when
        ResultActions resultActions = mockMvc.perform(get("/bookings/available")
                .queryParam("serviceId", serviceId)
                .queryParam("date", date)
                .queryParam("serviceDuration", serviceDuration)
        );
        // then
        resultActions
                .andExpect(status().is(expectedStatus));

        bodyChecker.accept(resultActions, expectedTimeRanges);
    }

    private static Stream<Arguments> getAvailableTimeRangesArguments() {
        return Stream.of(
                Arguments.of(
                        "Incorrect serviceId UUID",
                        "54218760-ae5d-45b9-9ceb-58d36d86902X",
                        "2025-01-01",
                        "15",
                        List.of(),
                        400,
                        (BiConsumer<ResultActions, List<TimeRange>>) (resultActions, _) -> validateProblemDetail(resultActions,
                                "Bad Request",
                                400,
                                "Parameter: 'serviceId' has incorrect value: '54218760-ae5d-45b9-9ceb-58d36d86902X'"
                        )
                ),
                Arguments.of(
                        "Incorrect serviceDuration",
                        "54218760-ae5d-45b9-9ceb-58d36d869028",
                        "2025-01-01",
                        "0",
                        List.of(),
                        400,
                        (BiConsumer<ResultActions, List<TimeRange>>) (resultActions, _) -> validateProblemDetail(resultActions,
                                "Bad Request",
                                400,
                                "getAvailableTimeRanges.serviceDuration: must be greater than 0"
                        )
                ),
                Arguments.of(
                        "Correct response",
                        "54218760-ae5d-45b9-9ceb-58d36d869028",
                        "2025-01-01",
                        "15",
                        List.of(
                                TimeRange.of(30, 45),
                                TimeRange.of(60, 120),
                                TimeRange.of(200, 300)
                        ),
                        200,
                        (BiConsumer<ResultActions, List<TimeRange>>) BookingControllerTest::validateTimeRangesResponse
                )
        );
    }

    @SneakyThrows
    private static void validateTimeRangesResponse(ResultActions resultActions, List<TimeRange> timeRanges) {
        resultActions = resultActions
                .andExpect(jsonPath("$.length()").value(timeRanges.size()));

        for (int i = 0; i < timeRanges.size(); i++) {
            TimeRange timeRange = timeRanges.get(i);

            resultActions
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath(String.format("$[%d].length()", i)).value(2))
                    .andExpect(jsonPath(String.format("$[%d].start", i)).value(timeRange.start().minuteOfDay()))
                    .andExpect(jsonPath(String.format("$[%d].end", i)).value(timeRange.end().minuteOfDay()));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getUserBookingTimeRangeArguments")
    void getUserBookingTimeRange(
            String name,
            String serviceId,
            String date,
            String userId,
            String bookingId,
            TimeRange expectedTimeRange,
            int expectedStatus,
            BiConsumer<ResultActions, TimeRange> bodyChecker
    ) throws Exception {
        // given
        GetUserBookingQuery query = GetUserBookingQuery.of(
                UUID.fromString(serviceId),
                LocalDate.parse(date),
                UUID.fromString(userId),
                UUID.fromString(bookingId)
        );
        if (expectedTimeRange != null) {
            when(service.getUserBookingTimeRange(query)).thenReturn(expectedTimeRange);
        } else {
            when(service.getUserBookingTimeRange(query)).thenThrow(new UserBookingNotFoundException(query));
        }
        // when
        ResultActions resultActions = mockMvc.perform(get("/bookings/user")
                .queryParam("serviceId", serviceId)
                .queryParam("date", date)
                .queryParam("userId", userId)
                .queryParam("bookingId", bookingId)
        );
        // then
        resultActions
                .andExpect(status().is(expectedStatus));

        bodyChecker.accept(resultActions, expectedTimeRange);
    }

    private static Stream<Arguments> getUserBookingTimeRangeArguments() {
        return Stream.of(
                Arguments.of(
                        "User booking not found exception",
                        "54218760-ae5d-45b9-9ceb-58d36d869027",
                        "2025-01-02",
                        "54218760-ae5d-45b9-9ceb-58d36d869028",
                        "54218760-ae5d-45b9-9ceb-58d36d869029",
                        null,
                        404,
                        (BiConsumer<ResultActions, TimeRange>) (resultActions, _) -> validateProblemDetail(resultActions,
                                "Not Found",
                                404,
                                "UserBooking(date: 2025-01-02, serviceId: 54218760-ae5d-45b9-9ceb-58d36d869027, bookingId: 54218760-ae5d-45b9-9ceb-58d36d869029) was not found."
                        )
                ),
                Arguments.of(
                        "Correct response",
                        "54218760-ae5d-45b9-9ceb-58d36d869027",
                        "2025-01-01",
                        "54218760-ae5d-45b9-9ceb-58d36d869028",
                        "54218760-ae5d-45b9-9ceb-58d36d869029",
                        TimeRange.of(30, 45),
                        200,
                        (BiConsumer<ResultActions, TimeRange>) BookingControllerTest::validateTimeRangeResponse
                )
        );
    }

    @SneakyThrows
    private static void validateTimeRangeResponse(ResultActions resultActions, TimeRange timeRange) {
        resultActions
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.start").value(timeRange.start().minuteOfDay()))
                .andExpect(jsonPath("$.end").value(timeRange.end().minuteOfDay()));
    }

    @SneakyThrows
    private static void validateProblemDetail(
            ResultActions resultActions,
            String title,
            int status,
            String detail
    ) {
        resultActions
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    void getUserBookingTimeRange() {
    }

    @Test
    void getUserBookings() {
    }
}