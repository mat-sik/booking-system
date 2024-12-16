package com.github.matsik.booking.controller;

import com.github.matsik.booking.client.command.CommandRemoteService;
import com.github.matsik.booking.client.query.QueryRemoteService;
import com.github.matsik.booking.config.jackson.JacksonConfiguration;
import com.github.matsik.mongo.model.Booking;
import com.github.matsik.query.response.ServiceBookingResponse;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
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
    private QueryRemoteService queryService;


    @MockitoBean
    private CommandRemoteService commandService;

    @Test
    void createBooking() {
    }

    @Test
    void deleteBooking() {
    }

    @Test
    void getAvailableTimeRanges() {
    }

    @Test
    void getUserBooking() {
    }

    private static Stream<Arguments> provideGetBookingsTestCases() {
        return Stream.of(
                Arguments.of(
                        "OK response.",
                        toStringParam(COMMON_DATES),
                        toStringParam(COMMON_SERVICE_IDS),
                        toStringParam(COMMON_USER_IDS),
                        (MockServiceSetUp<QueryRemoteService>) (service, args) -> {
                            List<LocalDate> dates = toLocalDate((String) args[0]);
                            List<ObjectId> serviceIds = toObjectId((String) args[1]);
                            List<ObjectId> userIds = toObjectId((String) args[2]);

                            when(service.getBookings(dates, serviceIds, userIds))
                                    .thenReturn(COMMON_SERVICE_BOOKING_RESPONSES);
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, args) -> {
                            List<LocalDate> dates = toLocalDate((String) args[0]);
                            List<ObjectId> serviceIds = toObjectId((String) args[1]);
                            List<ObjectId> userIds = toObjectId((String) args[2]);

                            then(service).should().getBookings(dates, serviceIds, userIds);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        COMMON_MOCK_MVC_GET_BOOKINGS_EXPECTATION_ASSERTION
                ),
                Arguments.of(
                        "Ok response, empty string params.",
                        "",
                        "",
                        "",
                        (MockServiceSetUp<QueryRemoteService>) (service, args) -> {
                            List<LocalDate> dates = toLocalDate((String) args[0]);
                            List<ObjectId> serviceIds = toObjectId((String) args[1]);
                            List<ObjectId> userIds = toObjectId((String) args[2]);

                            when(service.getBookings(dates, serviceIds, userIds))
                                    .thenReturn(COMMON_SERVICE_BOOKING_RESPONSES);
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, args) -> {
                            List<LocalDate> dates = toLocalDate((String) args[0]);
                            List<ObjectId> serviceIds = toObjectId((String) args[1]);
                            List<ObjectId> userIds = toObjectId((String) args[2]);

                            then(service).should().getBookings(dates, serviceIds, userIds);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        COMMON_MOCK_MVC_GET_BOOKINGS_EXPECTATION_ASSERTION
                ),
                Arguments.of(
                        "Ok response, params not set.",
                        null,
                        null,
                        null,
                        (MockServiceSetUp<QueryRemoteService>) (service, args) -> {
                            List<LocalDate> dates = toLocalDate((String) args[0]);
                            List<ObjectId> serviceIds = toObjectId((String) args[1]);
                            List<ObjectId> userIds = toObjectId((String) args[2]);

                            when(service.getBookings(dates, serviceIds, userIds))
                                    .thenReturn(COMMON_SERVICE_BOOKING_RESPONSES);
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, args) -> {
                            List<LocalDate> dates = toLocalDate((String) args[0]);
                            List<ObjectId> serviceIds = toObjectId((String) args[1]);
                            List<ObjectId> userIds = toObjectId((String) args[2]);

                            then(service).should().getBookings(dates, serviceIds, userIds);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        COMMON_MOCK_MVC_GET_BOOKINGS_EXPECTATION_ASSERTION
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
            MockServiceSetUp<QueryRemoteService> mockServiceSetUp,
            MockServiceAssertion<QueryRemoteService> mockServiceAssertion,
            MockMvcExpectationAssertion mockMvcExpectationAssertion
    ) throws Exception {
        // given
        mockServiceSetUp.setUp(queryService, dates, serviceIds, userIds);

        // then
        MockHttpServletRequestBuilder requestBuilder = getGetBookingsRequest(dates, serviceIds, userIds);
        ResultActions resultActions = mockMvc.perform(requestBuilder);

        // when
        mockMvcExpectationAssertion.assertExpectations(resultActions);

        mockServiceAssertion.assertMock(queryService, dates, serviceIds, userIds);
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

    private static final List<ServiceBookingResponse> COMMON_SERVICE_BOOKING_RESPONSES = List.of(
            new ServiceBookingResponse(
                    COMMON_SERVICE_BOOKING_IDS.get(0),
                    COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    COMMON_SERVICE_IDS.get(0),
                    List.of(
                            COMMONG_BOOKINGS.get(0),
                            COMMONG_BOOKINGS.get(1)
                    )
            ),
            new ServiceBookingResponse(
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
                .andExpect(jsonPath("$.length()").value(COMMON_SERVICE_BOOKING_RESPONSES.size()));

        for (int i = 0; i < COMMON_SERVICE_BOOKING_RESPONSES.size(); i++) {
            ServiceBookingResponse serviceBooking = COMMON_SERVICE_BOOKING_RESPONSES.get(i);
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
        void setUp(T service, Object... args);
    }

    private interface MockServiceAssertion<T> {
        void assertMock(T service, Object... args);
    }

    @FunctionalInterface
    private interface MockMvcExpectationAssertion {
        void assertExpectations(ResultActions resultActions) throws Exception;
    }
}