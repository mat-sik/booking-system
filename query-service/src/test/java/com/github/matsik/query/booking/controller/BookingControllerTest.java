package com.github.matsik.query.booking.controller;

import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.booking.TestDataGenerator;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetFirstUserBookingsQuery;
import com.github.matsik.query.booking.query.GetNextUserBookingsQuery;
import com.github.matsik.query.booking.query.GetUserBookingQuery;
import com.github.matsik.query.booking.query.GetUserBookingsQuery;
import com.github.matsik.query.booking.repository.projection.UserBooking;
import com.github.matsik.query.booking.service.BookingService;
import com.github.matsik.query.booking.service.exception.UserBookingNotFoundException;
import lombok.SneakyThrows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

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
                        incorrectServiceId(),
                        date(),
                        "15",
                        List.of(),
                        400,
                        (BiConsumer<ResultActions, List<TimeRange>>) (resultActions, _) -> validateProblemDetail(resultActions,
                                "Bad Request",
                                400,
                                String.format("Parameter: 'serviceId' has incorrect value: '%s'", incorrectServiceId())
                        )
                ),
                Arguments.of(
                        "Incorrect serviceDuration",
                        serviceId(),
                        date(),
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
                        serviceId(),
                        date(),
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

    private static String incorrectServiceId() {
        return "54218760-ae5d-45b9-9ceb-58d36d86902X";
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
                        serviceId(),
                        date(),
                        userId(),
                        bookingId(),
                        null,
                        404,
                        (BiConsumer<ResultActions, TimeRange>) (resultActions, _) -> validateProblemDetail(resultActions,
                                "Not Found",
                                404,
                                String.format("UserBooking(date: %s, serviceId: %s, bookingId: %s) was not found.", date(), serviceId(), bookingId())
                        )
                ),
                Arguments.of(
                        "Correct response",
                        serviceId(),
                        date(),
                        userId(),
                        bookingId(),
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("getUserBookingsArguments")
    void getUserBookings(
            String name,
            String userId,
            String serviceId,
            String date,
            String bookingId,
            String limit,
            List<UserBooking> expectedUserBookings,
            int expectedStatus,
            BiConsumer<ResultActions, List<UserBooking>> bodyChecker
    ) throws Exception {
        // given
        if (!expectedUserBookings.isEmpty()) {
            GetUserBookingsQuery query = query(userId, serviceId, date, bookingId, limit);
            when(service.getUserBookings(query)).thenReturn(expectedUserBookings);
        }
        // when
        ResultActions resultActions = mockMvc.perform(bookings(userId, serviceId, date, bookingId, limit));
        // then
        resultActions
                .andExpect(status().is(expectedStatus));

        bodyChecker.accept(resultActions, expectedUserBookings);
    }

    private static Stream<Arguments> getUserBookingsArguments() {
        return Stream.of(
                Arguments.of(
                        "Missing required parameter",
                        null,
                        serviceId(),
                        date(),
                        bookingId(),
                        "10",
                        List.of(),
                        400,
                        (BiConsumer<ResultActions, List<UserBooking>>) (resultActions, _) -> validateProblemDetail(resultActions,
                                "Bad Request",
                                400,
                                "Required request parameter 'userId' for method parameter type UUID is not present"
                        )
                ),
                Arguments.of(
                        "Correct response for next bookings",
                        userId(),
                        serviceId(),
                        date(),
                        bookingId(),
                        "10",
                        List.of(
                                userBooking(30, 60),
                                userBooking(90, 120)
                        ),
                        200,
                        (BiConsumer<ResultActions, List<UserBooking>>) BookingControllerTest::validateUserBookingsResponse
                ),
                Arguments.of(
                        "Correct response for first bookings",
                        userId(),
                        serviceId(),
                        date(),
                        null,
                        "10",
                        List.of(
                                userBooking(30, 60),
                                userBooking(90, 120)
                        ),
                        200,
                        (BiConsumer<ResultActions, List<UserBooking>>) BookingControllerTest::validateUserBookingsResponse
                )
        );
    }

    private static UserBooking userBooking(int start, int end) {
        return UserBooking.builder()
                .serviceId(UUID.fromString(serviceId()))
                .date(LocalDate.parse(date()))
                .bookingId(UUID.fromString(bookingId()))
                .timeRange(TimeRange.of(start, end))
                .build();
    }

    private GetUserBookingsQuery query(
            String userIdStr,
            String serviceIdStr,
            String dateStr,
            String bookingIdStr,
            String limitStr
    ) {
        UUID userId = UUID.fromString(userIdStr);
        int limit = Integer.parseInt(limitStr);
        if (serviceIdStr != null && dateStr != null && bookingIdStr != null) {
            return new GetNextUserBookingsQuery(
                    userId,
                    UUID.fromString(serviceIdStr),
                    LocalDate.parse(dateStr),
                    UUID.fromString(bookingIdStr),
                    limit
            );
        }
        return new GetFirstUserBookingsQuery(userId, limit);
    }

    private RequestBuilder bookings(
            String userId,
            String serviceId,
            String date,
            String bookingId,
            String limit
    ) {
        MockHttpServletRequestBuilder requestBuilder = get("/bookings");
        if (userId != null) {
            requestBuilder
                    .queryParam("userId", userId);
        }
        if (serviceId != null) {
            requestBuilder
                    .queryParam("cursorServiceId", serviceId);
        }
        if (date != null) {
            requestBuilder
                    .queryParam("cursorDate", date);
        }
        if (bookingId != null) {
            requestBuilder
                    .queryParam("cursorBookingId", bookingId);
        }
        if (limit != null) {
            requestBuilder
                    .queryParam("limit", limit);
        }
        return requestBuilder;
    }

    @SneakyThrows
    private static void validateUserBookingsResponse(ResultActions resultActions, List<UserBooking> userBookings) {
        resultActions = resultActions
                .andExpect(jsonPath("$.length()").value(userBookings.size()));

        for (int i = 0; i < userBookings.size(); i++) {
            UserBooking userBooking = userBookings.get(i);

            resultActions
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath(String.format("$[%d].length()", i)).value(5))
                    .andExpect(jsonPath(String.format("$[%d].serviceId", i)).value(userBooking.serviceId().toString()))
                    .andExpect(jsonPath(String.format("$[%d].date", i)).value(userBooking.date().toString()))
                    .andExpect(jsonPath(String.format("$[%d].bookingId", i)).value(userBooking.bookingId().toString()))
                    .andExpect(jsonPath(String.format("$[%d].start", i)).value(userBooking.timeRange().start().minuteOfDay()))
                    .andExpect(jsonPath(String.format("$[%d].end", i)).value(userBooking.timeRange().end().minuteOfDay()));
        }
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

    public static String userId() {
        return TestDataGenerator.numberToUUID(1).toString();
    }

    public static String serviceId() {
        return TestDataGenerator.numberToUUID(2).toString();
    }

    public static String bookingId() {
        return TestDataGenerator.numberToUUID(3).toString();
    }

    public static String date() {
        return TestDataGenerator.numberToLocalDate(1).toString();
    }
}