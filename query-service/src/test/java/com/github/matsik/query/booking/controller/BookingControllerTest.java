package com.github.matsik.query.booking.controller;

import com.github.matsik.mongo.model.Booking;
import com.github.matsik.query.booking.model.ServiceBooking;
import com.github.matsik.query.booking.model.UserBooking;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingQuery;
import com.github.matsik.query.booking.query.GetBookingsQuery;
import com.github.matsik.query.booking.service.BookingService;
import com.github.matsik.query.booking.service.TimeRange;
import com.github.matsik.query.booking.service.exception.UserBookingNotFoundException;
import com.github.matsik.query.config.jackson.JacksonConfiguration;
import org.bson.types.ObjectId;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(BookingController.class)
@Import(JacksonConfiguration.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService service;

    private static Stream<Arguments> provideGetAvailableTimeRangesTestCases() {
        return Stream.of(
                Arguments.of(
                        "OK response.",
                        COMMON_SERVICE_BOOKINGS.get(0).date(),
                        COMMON_SERVICE_BOOKINGS.get(0).serviceId().toHexString(),
                        String.valueOf(75),
                        (MockServiceSetUp<GetAvailableTimeRangesQuery>) (service, query) ->
                                when(service.getAvailableTimeRanges(query))
                                        .thenReturn(COMMON_TIME_RANGES),
                        (MockServiceAssertion<GetAvailableTimeRangesQuery>) (service, query) -> {
                            then(service).should().getAvailableTimeRanges(query);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions
                                    .andExpect(status().isOk())
                                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                    .andExpect(jsonPath("$").isArray())
                                    .andExpect(jsonPath("$.length()").value(COMMON_TIME_RANGES.size()));

                            for (int i = 0; i < COMMON_TIME_RANGES.size(); i++) {
                                resultActions
                                        .andExpect(jsonPath("$[%d].start", i).value(COMMON_TIME_RANGES.get(i).start()))
                                        .andExpect(jsonPath("$[%d].end", i).value(COMMON_TIME_RANGES.get(i).end()))
                                        .andExpect(jsonPath(String.format("$[%d]", i), aMapWithSize(2)));
                            }
                        }
                ),
                Arguments.of(
                        "Invalid service duration number format.",
                        COMMON_SERVICE_BOOKINGS.get(0).date(),
                        COMMON_SERVICE_BOOKINGS.get(0).serviceId().toHexString(),
                        "invalid format",
                        (MockServiceSetUp<GetAvailableTimeRangesQuery>) (service, query) -> {
                        },
                        (MockServiceAssertion<GetAvailableTimeRangesQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "For input string: \"invalidformat\"",
                                    "/booking/available"
                            );
                        }
                ),
                Arguments.of(
                        "Constraint violation of service duration.",
                        COMMON_SERVICE_BOOKINGS.get(0).date(),
                        COMMON_SERVICE_BOOKINGS.get(0).serviceId().toHexString(),
                        String.valueOf(-1),
                        (MockServiceSetUp<GetAvailableTimeRangesQuery>) (service, query) -> {
                        },
                        (MockServiceAssertion<GetAvailableTimeRangesQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "getAvailableTimeRanges.serviceDuration: must be greater than 0",
                                    "/booking/available"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect hex string for serviceId",
                        COMMON_SERVICE_BOOKINGS.get(0).date(),
                        "Incorrect hex string",
                        String.valueOf(75),
                        (MockServiceSetUp<GetAvailableTimeRangesQuery>) (service, query) -> {
                        },
                        (MockServiceAssertion<GetAvailableTimeRangesQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Invalid ObjectId: Incorrect hex string",
                                    "/booking/available"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect date string format.",
                        "22004-10-33",
                        COMMON_SERVICE_BOOKINGS.get(0).serviceId().toHexString(),
                        String.valueOf(75),
                        (MockServiceSetUp<GetAvailableTimeRangesQuery>) (service, query) -> {
                        },
                        (MockServiceAssertion<GetAvailableTimeRangesQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions
                                    .andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Parse attempt failed for value [22004-10-33]",
                                    "/booking/available"
                            );
                        }
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
            MockServiceSetUp<GetAvailableTimeRangesQuery> mockServiceSetUp,
            MockServiceAssertion<GetAvailableTimeRangesQuery> mockServiceAssertion,
            MockMvcExpectationAssertion mockMvcExpectationAssertion
    ) throws Exception {
        // given
        GetAvailableTimeRangesQuery query = getGetAvailableTimeRangesQueryOrDefault(date, serviceId, serviceDuration);

        mockServiceSetUp.setUp(service, query);

        // when
        ResultActions resultActions = mockMvc.perform(get("/booking/available")
                .param("date", date)
                .param("serviceId", serviceId)
                .param("serviceDuration", serviceDuration)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        mockMvcExpectationAssertion.assertExpectations(resultActions);

        mockServiceAssertion.assertMock(service, query);
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

    private static Stream<Arguments> provideGetUserBookingTestCases() {
        return Stream.of(
                Arguments.of(
                        "Ok response.",
                        COMMON_SERVICE_BOOKINGS.get(0).date(),
                        COMMON_SERVICE_BOOKINGS.get(0).serviceId().toHexString(),
                        COMMON_SERVICE_BOOKINGS.get(0).bookings().get(0).id().toHexString(),
                        (MockServiceSetUp<GetBookingQuery>) (service, query) ->
                                when(service.getUserBooking(query)).thenReturn(COMMON_USER_BOOKING),
                        (MockServiceAssertion<GetBookingQuery>) (service, query) -> {
                            then(service).should().getUserBooking(query);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        (MockMvcExpectationAssertion) (resultActions) ->
                                resultActions
                                        .andExpect(status().isOk())
                                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(jsonPath("$", aMapWithSize(3)))
                                        .andExpect(jsonPath("$.userId").value(COMMON_USER_BOOKING.userId().toHexString()))
                                        .andExpect(jsonPath("$.start").value(String.valueOf(COMMON_USER_BOOKING.start())))
                                        .andExpect(jsonPath("$.end").value(String.valueOf(COMMON_USER_BOOKING.end())))
                ),
                Arguments.of(
                        "Incorrect date string format.",
                        "22004-10-33",
                        COMMON_SERVICE_BOOKINGS.get(0).serviceId().toHexString(),
                        COMMON_SERVICE_BOOKINGS.get(0).bookings().get(0).id().toHexString(),
                        (MockServiceSetUp<GetBookingQuery>) (service, query) -> {
                        },
                        (MockServiceAssertion<GetBookingQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Parse attempt failed for value [22004-10-33]",
                                    "/booking"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect hex string for serviceId.",
                        COMMON_SERVICE_BOOKINGS.get(0).date(),
                        "foo",
                        COMMON_SERVICE_BOOKINGS.get(0).bookings().get(0).id().toHexString(),
                        (MockServiceSetUp<GetBookingQuery>) (service, query) -> {
                        },
                        (MockServiceAssertion<GetBookingQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Invalid ObjectId: foo",
                                    "/booking"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect hex string for bookingId.",
                        COMMON_SERVICE_BOOKINGS.get(0).date(),
                        COMMON_SERVICE_BOOKINGS.get(0).serviceId().toHexString(),
                        "bar",
                        (MockServiceSetUp<GetBookingQuery>) (service, query) -> {
                        },
                        (MockServiceAssertion<GetBookingQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Invalid ObjectId: bar",
                                    "/booking"
                            );
                        }
                ),
                Arguments.of(
                        "User Booking not found.",
                        COMMON_SERVICE_BOOKINGS.get(0).date(),
                        COMMON_SERVICE_BOOKINGS.get(0).serviceId().toHexString(),
                        COMMON_SERVICE_BOOKINGS.get(1).bookings().get(0).id().toHexString(),
                        (MockServiceSetUp<GetBookingQuery>) (service, query) ->
                                when(service.getUserBooking(query)).thenThrow(new UserBookingNotFoundException(query)),
                        (MockServiceAssertion<GetBookingQuery>) (service, query) -> {
                            then(service).should().getUserBooking(query);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isNotFound())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Not Found",
                                    404,
                                    "UserBooking(date: 2024-12-12, serviceId: 010000000000000000000000, bookingId: 100000000000000000000002) was not found.",
                                    "/booking"
                            );
                        }
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetUserBookingTestCases")
    void getUserBooking(
            String name,
            String date,
            String serviceId,
            String bookingId,
            MockServiceSetUp<GetBookingQuery> mockServiceSetUp,
            MockServiceAssertion<GetBookingQuery> mockServiceAssertion,
            MockMvcExpectationAssertion mockMvcExpectationAssertion
    ) throws Exception {
        // given
        GetBookingQuery query = getGetBookingQueryOrDefault(date, serviceId, bookingId);
        mockServiceSetUp.setUp(service, query);

        // when
        ResultActions resultActions = mockMvc.perform(get("/booking")
                .param("date", date)
                .param("serviceId", serviceId)
                .param("bookingId", bookingId)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        mockMvcExpectationAssertion.assertExpectations(resultActions);

        mockServiceAssertion.assertMock(service, query);
    }

    private static GetBookingQuery getGetBookingQueryOrDefault(
            String dateString,
            String serviceIdString,
            String bookingIdString
    ) {
        try {
            LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
            ObjectId serviceId = new ObjectId(serviceIdString);
            ObjectId bookingId = new ObjectId(bookingIdString);

            return GetBookingQuery.Factory.create(date, serviceId, bookingId);
        } catch (Exception ex) {
            return GetBookingQuery.Factory.create(
                    LocalDate.of(2000, 1, 1),
                    new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa"),
                    new ObjectId("bbbbbbbbbbbbbbbbbbbbbbbb")
            );
        }
    }

    private static Stream<Arguments> provideGetBookingsTestCases() {
        return Stream.of(
                Arguments.of(
                        "Ok response.",
                        toStringParam(COMMON_DATES),
                        toStringParam(COMMON_SERVICE_IDS),
                        toStringParam(COMMON_USER_IDS),
                        (MockServiceSetUp<GetBookingsQuery>) (service, query) ->
                                when(service.getBookings(query)).thenReturn(COMMON_SERVICE_BOOKINGS),
                        (MockServiceAssertion<GetBookingsQuery>) (service, query) -> {
                            then(service).should().getBookings(query);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        COMMON_MOCK_MVC_GET_BOOKINGS_EXPECTATION_ASSERTION
                ),
                Arguments.of(
                        "Ok response, empty string params.",
                        "",
                        "",
                        "",
                        (MockServiceSetUp<GetBookingsQuery>) (service, query) ->
                                when(service.getBookings(query)).thenReturn(COMMON_SERVICE_BOOKINGS),
                        (MockServiceAssertion<GetBookingsQuery>) (service, query) -> {
                            then(service).should().getBookings(query);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        COMMON_MOCK_MVC_GET_BOOKINGS_EXPECTATION_ASSERTION
                ),
                Arguments.of(
                        "Ok response, params not set.",
                        null,
                        null,
                        null,
                        (MockServiceSetUp<GetBookingsQuery>) (service, query) ->
                                when(service.getBookings(query)).thenReturn(COMMON_SERVICE_BOOKINGS),
                        (MockServiceAssertion<GetBookingsQuery>) (service, query) -> {
                            then(service).should().getBookings(query);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        COMMON_MOCK_MVC_GET_BOOKINGS_EXPECTATION_ASSERTION
                ),
                Arguments.of(
                        "Incorrect date string format.",
                        toStringParam(COMMON_DATES) + ",foo",
                        null,
                        null,
                        (MockServiceSetUp<GetBookingsQuery>) (service, query) -> {
                        },
                        (MockServiceAssertion<GetBookingsQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Parse attempt failed for value [2024-12-12,2024-12-13,foo]",
                                    "/booking/all"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect hex string for serviceId.",
                        null,
                        COMMON_SERVICE_IDS + ",foo",
                        null,
                        (MockServiceSetUp<GetBookingsQuery>) (service, query) -> {
                        },
                        (MockServiceAssertion<GetBookingsQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Invalid ObjectId: [010000000000000000000000, 010000000000000000000001],foo",
                                    "/booking/all"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect hex string for userId.",
                        null,
                        null,
                        COMMON_USER_IDS + ",foo",
                        (MockServiceSetUp<GetBookingsQuery>) (service, query) -> {
                        },
                        (MockServiceAssertion<GetBookingsQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Invalid ObjectId: [110000000000000000000000, 110000000000000000000001],foo",
                                    "/booking/all"
                            );
                        }
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetBookingsTestCases")
    void getBookings(
            String name,
            String dates,
            String serviceIds,
            String userIds,
            MockServiceSetUp<GetBookingsQuery> mockServiceSetUp,
            MockServiceAssertion<GetBookingsQuery> mockServiceAssertion,
            MockMvcExpectationAssertion mockMvcExpectationAssertion
    ) throws Exception {
        // given
        GetBookingsQuery query = getGetBookingQuery(dates, serviceIds, userIds);

        mockServiceSetUp.setUp(service, query);

        // then
        MockHttpServletRequestBuilder requestBuilder = getGetBookingsRequest(dates, serviceIds, userIds);
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        // when
        mockMvcExpectationAssertion.assertExpectations(resultActions);

        mockServiceAssertion.assertMock(service, query);
    }

    private static GetBookingsQuery getGetBookingQuery(
            String datesString,
            String serviceIdsString,
            String userIdsString
    ) {
        List<LocalDate> dates = toLocalDate(datesString);
        List<ObjectId> serviceIds = toObjectId(serviceIdsString);
        List<ObjectId> userIds = toObjectId(userIdsString);
        return new GetBookingsQuery(dates, serviceIds, userIds);
    }

    private MockHttpServletRequestBuilder getGetBookingsRequest(
            String dates,
            String serviceIds,
            String userIds
    ) {
        MockHttpServletRequestBuilder request = get("/booking/all")
                .contentType(MediaType.APPLICATION_JSON);

        if (dates != null) {
            request.param("dates", dates);
        }
        if (serviceIds != null) {
            request.param("serviceIds", serviceIds);
        }
        if (userIds != null) {
            request.param("userIds", userIds);
        }

        return request;
    }

    private static <T> String toStringParam(List<T> elements) {
        return String.join(",", elements.stream().map(Object::toString).toList());
    }

    private static List<LocalDate> toLocalDate(String elements) {
        return toType(elements, (el) -> LocalDate.parse(el, DateTimeFormatter.ISO_LOCAL_DATE));
    }

    private static List<ObjectId> toObjectId(String elements) {
        return toType(elements, ObjectId::new);
    }

    private static <T> List<T> toType(String elements, Function<String, T> mapper) {
        try {
            if (elements == null || elements.isBlank()) {
                return List.of();
            }
            return Arrays.stream(elements.split(","))
                    .map(mapper)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private interface MockServiceSetUp<T> {
        void setUp(BookingService service, T query);
    }

    private interface MockServiceAssertion<T> {
        void assertMock(BookingService service, T query);
    }

    @FunctionalInterface
    private interface MockMvcExpectationAssertion {
        void assertExpectations(ResultActions resultActions) throws Exception;
    }

    private static void assertProblemDetailExpectations(
            ResultActions resultActions,
            String type,
            String title,
            int status,
            String detail,
            String instance
    ) throws Exception {
        resultActions.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$", aMapWithSize(6)))
                .andExpect(jsonPath("$.type").value(type))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value(instance))
                .andExpect(jsonPath("$.properties").isEmpty());
    }

    private static final List<TimeRange> COMMON_TIME_RANGES = List.of(
            new TimeRange(900, 990),
            new TimeRange(990, 1080)
    );

    private static final List<ObjectId> COMMON_SERVICE_BOOKING_IDS = List.of(
            new ObjectId("000000000000000000000000"),
            new ObjectId("000000000000000000000001")
    );

    private static final List<ObjectId> COMMON_BOOKING_IDS = List.of(
            new ObjectId("100000000000000000000000"),
            new ObjectId("100000000000000000000001"),
            new ObjectId("100000000000000000000002"),
            new ObjectId("100000000000000000000003")
    );

    private static final List<ObjectId> COMMON_SERVICE_IDS = List.of(
            new ObjectId("010000000000000000000000"),
            new ObjectId("010000000000000000000001")
    );

    private static final List<ObjectId> COMMON_USER_IDS = List.of(
            new ObjectId("110000000000000000000000"),
            new ObjectId("110000000000000000000001")
    );

    private static final List<LocalDate> COMMON_DATES = List.of(
            LocalDate.of(2024, 12, 12),
            LocalDate.of(2024, 12, 13)
    );

    private static final List<Booking> COMMONG_BOOKINGS = List.of(
            new Booking(
                    COMMON_BOOKING_IDS.get(0),
                    COMMON_USER_IDS.get(0),
                    0,
                    30
            ),
            new Booking(
                    COMMON_BOOKING_IDS.get(1),
                    COMMON_USER_IDS.get(1),
                    30,
                    60
            ),
            new Booking(
                    COMMON_BOOKING_IDS.get(2),
                    COMMON_USER_IDS.get(0),
                    60,
                    90
            ),

            new Booking(
                    COMMON_BOOKING_IDS.get(3),
                    COMMON_USER_IDS.get(1),
                    90,
                    120
            )
    );

    private static final UserBooking COMMON_USER_BOOKING = new UserBooking(
            COMMONG_BOOKINGS.get(0).id(),
            COMMONG_BOOKINGS.get(0).start(),
            COMMONG_BOOKINGS.get(0).end()
    );

    private static final List<ServiceBooking> COMMON_SERVICE_BOOKINGS = List.of(
            new ServiceBooking(
                    COMMON_SERVICE_BOOKING_IDS.get(0),
                    COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    COMMON_SERVICE_IDS.get(0),
                    List.of(
                            COMMONG_BOOKINGS.get(0),
                            COMMONG_BOOKINGS.get(1)
                    )
            ),
            new ServiceBooking(
                    COMMON_SERVICE_BOOKING_IDS.get(1),
                    COMMON_DATES.get(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    COMMON_SERVICE_IDS.get(1),
                    List.of(
                            COMMONG_BOOKINGS.get(2),
                            COMMONG_BOOKINGS.get(3)
                    )
            )
    );

    private static final MockMvcExpectationAssertion COMMON_MOCK_MVC_GET_BOOKINGS_EXPECTATION_ASSERTION = (resultActions) -> {
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(COMMON_SERVICE_BOOKINGS.size()));

        for (int i = 0; i < COMMON_SERVICE_BOOKINGS.size(); i++) {
            ServiceBooking serviceBooking = COMMON_SERVICE_BOOKINGS.get(i);
            List<Booking> bookings = serviceBooking.bookings();

            resultActions
                    .andExpect(jsonPath(String.format("$[%d]", i), aMapWithSize(4)))
                    .andExpect(jsonPath("$[%d].id", i).value(serviceBooking.id().toHexString()))
                    .andExpect(jsonPath("$[%d].date", i).value(serviceBooking.date()))
                    .andExpect(jsonPath("$[%d].serviceId", i).value(serviceBooking.serviceId().toHexString()))
                    .andExpect(jsonPath("$[%d].bookings", i).isArray());

            for (int j = 0; j < bookings.size(); j++) {
                Booking booking = bookings.get(j);
                resultActions
                        .andExpect(jsonPath(String.format("$[%d].bookings[%d]", i, j), aMapWithSize(4)))
                        .andExpect(jsonPath(String.format("$[%d].bookings[%d].id", i, j)).value(booking.id().toHexString()))
                        .andExpect(jsonPath(String.format("$[%d].bookings[%d].userId", i, j)).value(booking.userId().toHexString()))
                        .andExpect(jsonPath(String.format("$[%d].bookings[%d].start", i, j)).value(booking.start()))
                        .andExpect(jsonPath(String.format("$[%d].bookings[%d].end", i, j)).value(booking.end()));
            }
        }
    };
}