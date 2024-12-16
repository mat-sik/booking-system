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
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
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
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        new ObjectId("000000000000000000000000").toHexString(),
                        String.valueOf(75),
                        List.of(
                                new TimeRange(900, 990),
                                new TimeRange(990, 1080)
                        ),
                        (MockMvcExpectationAssertion<List<TimeRange>>) (resultActions, availableTimeRanges) -> {
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
                        (MockServiceAssertion<GetAvailableTimeRangesQuery>) (service, query) -> {
                            then(service).should().getAvailableTimeRanges(query);
                            then(service).shouldHaveNoMoreInteractions();
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
                        (MockMvcExpectationAssertion<List<TimeRange>>) (resultActions, availableTimeRanges) -> {
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
                        },
                        (MockServiceAssertion<GetAvailableTimeRangesQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions()
                ),
                Arguments.of(
                        "Constraint violation of service duration.",
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        new ObjectId("000000000000000000000000").toHexString(),
                        String.valueOf(-1),
                        List.of(
                                new TimeRange(900, 990),
                                new TimeRange(990, 1080)
                        ),
                        (MockMvcExpectationAssertion<List<TimeRange>>) (resultActions, availableTimeRanges) -> {
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
                        },
                        (MockServiceAssertion<GetAvailableTimeRangesQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions()
                ),
                Arguments.of(
                        "Incorrect hex string for serviceId",
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        "Incorrect hex string",
                        String.valueOf(75),
                        List.of(
                                new TimeRange(900, 990),
                                new TimeRange(990, 1080)
                        ),
                        (MockMvcExpectationAssertion<List<TimeRange>>) (resultActions, availableTimeRanges) -> {
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
                        },
                        (MockServiceAssertion<GetAvailableTimeRangesQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions()
                ),
                Arguments.of(
                        "Incorrect date string format.",
                        "22004-10-33",
                        new ObjectId("000000000000000000000000").toHexString(),
                        String.valueOf(75),
                        List.of(
                                new TimeRange(900, 990),
                                new TimeRange(990, 1080)
                        ),
                        (MockMvcExpectationAssertion<List<TimeRange>>) (resultActions, availableTimeRanges) -> {
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
                        },
                        (MockServiceAssertion<GetAvailableTimeRangesQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions()
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
            MockMvcExpectationAssertion<List<TimeRange>> mockMvcExpectationAssertion,
            MockServiceAssertion<GetAvailableTimeRangesQuery> mockServiceAssertion
    ) throws Exception {
        // given
        GetAvailableTimeRangesQuery query = getGetAvailableTimeRangesQueryOrDefault(date, serviceId, serviceDuration);
        when(service.getAvailableTimeRanges(query))
                .thenReturn(availableTimeRanges);

        // when
        ResultActions resultActions = mockMvc.perform(get("/booking/available")
                .param("date", date)
                .param("serviceId", serviceId)
                .param("serviceDuration", serviceDuration)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        mockMvcExpectationAssertion.assertExpectations(resultActions, availableTimeRanges);

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
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        new ObjectId("000000000000000000000000").toHexString(),
                        new ObjectId("100000000000000000000000").toHexString(),
                        new UserBooking(
                                new ObjectId("110000000000000000000000"),
                                60,
                                120
                        ),
                        (MockMvcExpectationAssertion<UserBooking>) (resultActions, userBooking) ->
                                resultActions
                                        .andExpect(status().isOk())
                                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(jsonPath("$", aMapWithSize(3)))
                                        .andExpect(jsonPath("$.userId").value(userBooking.userId().toHexString()))
                                        .andExpect(jsonPath("$.start").value(String.valueOf(userBooking.start())))
                                        .andExpect(jsonPath("$.end").value(String.valueOf(userBooking.end()))),
                        (MockServiceSetUp) (service, query, userBooking) ->
                                when(service.getUserBooking(query)).thenReturn(userBooking),
                        (MockServiceAssertion<GetBookingQuery>) (service, query) ->
                                then(service).should().getUserBooking(query)

                ),
                Arguments.of(
                        "Incorrect date string format.",
                        "22004-10-33",
                        new ObjectId("000000000000000000000000").toHexString(),
                        new ObjectId("100000000000000000000000").toHexString(),
                        new UserBooking(
                                new ObjectId("110000000000000000000000"),
                                60,
                                120
                        ),
                        (MockMvcExpectationAssertion<UserBooking>) (resultActions, userBooking) -> {
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
                        },
                        (MockServiceSetUp) (service, query, userBooking) ->
                                when(service.getUserBooking(query)).thenReturn(userBooking),
                        (MockServiceAssertion<GetBookingQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions()

                ),
                Arguments.of(
                        "Incorrect hex string for serviceId.",
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        "foo",
                        new ObjectId("100000000000000000000000").toHexString(),
                        new UserBooking(
                                new ObjectId("110000000000000000000000"),
                                60,
                                120
                        ),
                        (MockMvcExpectationAssertion<UserBooking>) (resultActions, userBooking) -> {
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
                        },
                        (MockServiceSetUp) (service, query, userBooking) ->
                                when(service.getUserBooking(query)).thenReturn(userBooking),
                        (MockServiceAssertion<GetBookingQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions()

                ),
                Arguments.of(
                        "Incorrect hex string for bookingId.",
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        new ObjectId("000000000000000000000000").toHexString(),
                        "bar",
                        new UserBooking(
                                new ObjectId("110000000000000000000000"),
                                60,
                                120
                        ),
                        (MockMvcExpectationAssertion<UserBooking>) (resultActions, userBooking) -> {
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
                        },
                        (MockServiceSetUp) (service, query, userBooking) ->
                                when(service.getUserBooking(query)).thenReturn(userBooking),
                        (MockServiceAssertion<GetBookingQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions()
                ),
                Arguments.of(
                        "User Booking not found.",
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        new ObjectId("000000000000000000000000").toHexString(),
                        new ObjectId("100000000000000000000000").toHexString(),
                        new UserBooking(
                                new ObjectId("110000000000000000000000"),
                                60,
                                120
                        ),
                        (MockMvcExpectationAssertion<UserBooking>) (resultActions, userBooking) -> {
                            resultActions.andExpect(status().isNotFound())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Not Found",
                                    404,
                                    "UserBooking(date: 2024-12-12, serviceId: 000000000000000000000000, bookingId: 100000000000000000000000) was not found.",
                                    "/booking"
                            );
                        },
                        (MockServiceSetUp) (service, query, userBooking) ->
                                when(service.getUserBooking(query)).thenThrow(new UserBookingNotFoundException(query)),
                        (MockServiceAssertion<GetBookingQuery>) (service, query) -> {
                            then(service).should().getUserBooking(query);
                            then(service).shouldHaveNoMoreInteractions();
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
            UserBooking userBooking,
            MockMvcExpectationAssertion<UserBooking> mockMvcExpectationAssertion,
            MockServiceSetUp mockServiceSetUp,
            MockServiceAssertion<GetBookingQuery> mockServiceAssertion
    ) throws Exception {
        // given
        GetBookingQuery query = getGetBookingQueryOrDefault(date, serviceId, bookingId);
        mockServiceSetUp.setUp(service, query, userBooking);

        // when
        ResultActions resultActions = mockMvc.perform(get("/booking")
                .param("date", date)
                .param("serviceId", serviceId)
                .param("bookingId", bookingId)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        mockMvcExpectationAssertion.assertExpectations(resultActions, userBooking);

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

    private interface MockServiceSetUp {
        void setUp(BookingService service, GetBookingQuery query, UserBooking userBooking);
    }

    private static Stream<Arguments> provideGetBookingsTestCases() {
        return Stream.of(
                Arguments.of(
                        "Ok response.",
                        COMMON_DATES_STR,
                        COMMON_SERVICE_IDS_STR,
                        COMMON_USER_IDS_STR,
                        COMMON_SERVICE_BOOKINGS,
                        COMMON_MOCK_MVC_EXPECTATION_ASSERTION,
                        COMMON_MOCK_SERVICE_ASSERTION
                ),
                Arguments.of(
                        "Ok response, empty string params.",
                        "",
                        "",
                        "",
                        COMMON_SERVICE_BOOKINGS,
                        COMMON_MOCK_MVC_EXPECTATION_ASSERTION,
                        COMMON_MOCK_SERVICE_ASSERTION
                ),
                Arguments.of(
                        "Ok response, params not set.",
                        null,
                        null,
                        null,
                        COMMON_SERVICE_BOOKINGS,
                        COMMON_MOCK_MVC_EXPECTATION_ASSERTION,
                        COMMON_MOCK_SERVICE_ASSERTION
                ),
                Arguments.of(
                        "Incorrect date string format.",
                        COMMON_DATES_STR + ",foo",
                        null,
                        null,
                        List.of(),
                        (MockMvcExpectationAssertion<List<UserBooking>>) (resultActions, userBooking) -> {
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
                        },
                        (MockServiceAssertion<GetBookingsQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions()
                ),
                Arguments.of(
                        "Incorrect hex string for serviceId.",
                        null,
                        COMMON_SERVICE_IDS + ",foo",
                        null,
                        List.of(),
                        (MockMvcExpectationAssertion<List<UserBooking>>) (resultActions, userBooking) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Invalid ObjectId: [100000000000000000000000, 100000000000000000000001],foo",
                                    "/booking/all"
                            );
                        },
                        (MockServiceAssertion<GetBookingsQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions()
                ),
                Arguments.of(
                        "Incorrect hex string for userId.",
                        null,
                        null,
                        COMMON_USER_IDS + ",foo",
                        List.of(),
                        (MockMvcExpectationAssertion<List<UserBooking>>) (resultActions, userBooking) -> {
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
                        },
                        (MockServiceAssertion<GetBookingsQuery>) (service, query) ->
                                then(service).shouldHaveNoInteractions()
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
            List<ServiceBooking> serviceBookings,
            MockMvcExpectationAssertion<List<ServiceBooking>> mockMvcExpectationAssertion,
            MockServiceAssertion<GetBookingsQuery> mockServiceAssertion
    ) throws Exception {
        // given
        GetBookingsQuery query = getGetBookingQuery(dates, serviceIds, userIds);

        when(service.getBookings(query)).thenReturn(serviceBookings);

        // then
        MockHttpServletRequestBuilder requestBuilder = createGetBookingsRequest(dates, serviceIds, userIds);
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        // when
        mockMvcExpectationAssertion.assertExpectations(resultActions, serviceBookings);

        mockServiceAssertion.assertMock(service, query);
    }

    private static final List<LocalDate> COMMON_DATES = List.of(
            LocalDate.of(2024, 12, 12),
            LocalDate.of(2024, 12, 13)
    );

    private static final String COMMON_DATES_STR = toStringParam(COMMON_DATES);

    private static final List<ObjectId> COMMON_SERVICE_IDS = List.of(
            new ObjectId("100000000000000000000000"),
            new ObjectId("100000000000000000000001")
    );

    private static final String COMMON_SERVICE_IDS_STR = toStringParam(COMMON_SERVICE_IDS);

    private static final List<ObjectId> COMMON_USER_IDS = List.of(
            new ObjectId("010000000000000000000000"),
            new ObjectId("010000000000000000000001")
    );

    private static final String COMMON_USER_IDS_STR = toStringParam(COMMON_USER_IDS);

    private static final List<ServiceBooking> COMMON_SERVICE_BOOKINGS = List.of(
            new ServiceBooking(
                    new ObjectId("000000000000000000000000"),
                    COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    COMMON_SERVICE_IDS.get(0),
                    List.of(
                            new Booking(
                                    new ObjectId("110000000000000000000000"),
                                    COMMON_USER_IDS.get(0),
                                    0,
                                    30
                            ),
                            new Booking(
                                    new ObjectId("110000000000000000000001"),
                                    COMMON_USER_IDS.get(1),
                                    30,
                                    60
                            )
                    )
            ),
            new ServiceBooking(
                    new ObjectId("000000000000000000000001"),
                    COMMON_DATES.get(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    COMMON_SERVICE_IDS.get(1),
                    List.of(
                            new Booking(
                                    new ObjectId("110000000000000000000002"),
                                    COMMON_USER_IDS.get(0),
                                    30,
                                    60
                            ),
                            new Booking(
                                    new ObjectId("110000000000000000000003"),
                                    COMMON_USER_IDS.get(1),
                                    60, 90
                            )
                    )
            )
    );

    private static final MockMvcExpectationAssertion<List<ServiceBooking>> COMMON_MOCK_MVC_EXPECTATION_ASSERTION = (resultActions, serviceBookings) -> {
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(serviceBookings.size()));

        for (int i = 0; i < serviceBookings.size(); i++) {
            ServiceBooking serviceBooking = serviceBookings.get(i);
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

    private static final MockServiceAssertion<GetBookingsQuery> COMMON_MOCK_SERVICE_ASSERTION = (service, query) -> {
        then(service).should().getBookings(query);
        then(service).shouldHaveNoMoreInteractions();
    };

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

    private MockHttpServletRequestBuilder createGetBookingsRequest(
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
        try {
            if (elements == null || elements.isBlank()) {
                return List.of();
            }
            return Arrays.stream(elements.split(","))
                    .map(el -> LocalDate.parse(el, DateTimeFormatter.ISO_LOCAL_DATE))
                    .toList();
        } catch (DateTimeParseException ex) {
            return List.of();
        }
    }

    private static List<ObjectId> toObjectId(String elements) {
        try {
            if (elements == null || elements.isBlank()) {
                return List.of();
            }
            return Arrays.stream(elements.split(","))
                    .map(ObjectId::new)
                    .toList();
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    private interface MockServiceAssertion<T> {
        void assertMock(BookingService service, T query);
    }

    @FunctionalInterface
    private interface MockMvcExpectationAssertion<T> {
        void assertExpectations(ResultActions resultActions, T expectation) throws Exception;
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
}