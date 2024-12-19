package com.github.matsik.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.matsik.booking.client.command.CommandRemoteService;
import com.github.matsik.booking.client.command.exception.BookingCommandDeliveryException;
import com.github.matsik.booking.client.query.QueryRemoteService;
import com.github.matsik.booking.config.jackson.JacksonConfiguration;
import com.github.matsik.booking.controller.request.CreateBookingRequest;
import com.github.matsik.booking.controller.request.DeleteBookingRequest;
import com.github.matsik.mongo.model.Booking;
import com.github.matsik.query.response.ServiceBookingResponse;
import com.github.matsik.query.response.TimeRangeResponse;
import com.github.matsik.query.response.UserBookingResponse;
import feign.FeignException;
import feign.Request;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Autowired
    private ObjectMapper objectMapper;

    private static Stream<Arguments> provideCreateBookingTestCases() {
        return Stream.of(
                Arguments.of(
                        "OK response.",
                        COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        COMMON_SERVICE_IDS.get(0).toHexString(),
                        COMMON_USER_IDS.get(0).toHexString(),
                        "900",
                        "990",
                        (MockServiceSetUp<CommandRemoteService>) (service, args) -> {
                            CreateBookingRequest request = new CreateBookingRequest(
                                    LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE),
                                    new ObjectId((String) args[1]),
                                    new ObjectId((String) args[2]),
                                    Integer.parseInt((String) args[3]),
                                    Integer.parseInt((String) args[4])
                            );
                            doNothing().when(service).createBooking(request);
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, args) -> {
                            CreateBookingRequest request = new CreateBookingRequest(
                                    LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE),
                                    new ObjectId((String) args[1]),
                                    new ObjectId((String) args[2]),
                                    Integer.parseInt((String) args[3]),
                                    Integer.parseInt((String) args[4])
                            );
                            then(service).should().createBooking(request);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        (MockMvcExpectationAssertion) (resultActions) ->
                                resultActions.andExpect(status().isOk())
                ),
                Arguments.of(
                        "Incorrect date string format.",
                        "2024-12-32",
                        COMMON_SERVICE_IDS.get(0).toHexString(),
                        COMMON_USER_IDS.get(0).toHexString(),
                        "900",
                        "990",
                        (MockServiceSetUp<CommandRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, _) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "JSON parse error: Cannot deserialize value of type `java.time.LocalDate` from String \"2024-12-32\": Failed to deserialize java.time.LocalDate: (java.time.format.DateTimeParseException) Text '2024-12-32' could not be parsed: Invalid value for DayOfMonth (valid values 1 - 28/31): 32",
                                    "/booking/create"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect start string format.",
                        COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        COMMON_SERVICE_IDS.get(0).toHexString(),
                        COMMON_USER_IDS.get(0).toHexString(),
                        "invalid",
                        "990",
                        (MockServiceSetUp<CommandRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, _) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "JSON parse error: Cannot deserialize value of type `java.lang.Integer` from String \"invalid\": not a valid `java.lang.Integer` value",
                                    "/booking/create"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect end string format.",
                        COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        COMMON_SERVICE_IDS.get(0).toHexString(),
                        COMMON_USER_IDS.get(0).toHexString(),
                        "900",
                        "invalid",
                        (MockServiceSetUp<CommandRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, _) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "JSON parse error: Cannot deserialize value of type `java.lang.Integer` from String \"invalid\": not a valid `java.lang.Integer` value",
                                    "/booking/create"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect start string format, should be positive or zero.",
                        COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        COMMON_SERVICE_IDS.get(0).toHexString(),
                        COMMON_USER_IDS.get(0).toHexString(),
                        "-1",
                        "990",
                        (MockServiceSetUp<CommandRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, _) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Start must be 0 or greater",
                                    "/booking/create"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect end string format, end should be greater than 0 and start.",
                        COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        COMMON_SERVICE_IDS.get(0).toHexString(),
                        COMMON_USER_IDS.get(0).toHexString(),
                        "990",
                        "-1",
                        (MockServiceSetUp<CommandRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, _) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "End must be 0 or greater, Start must be less than End",
                                    "/booking/create"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect start and end string format, should be positive or zero and should not be the same.",
                        COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        COMMON_SERVICE_IDS.get(0).toHexString(),
                        COMMON_USER_IDS.get(0).toHexString(),
                        "-1",
                        "-1",
                        (MockServiceSetUp<CommandRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, _) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "End must be 0 or greater, Start must be 0 or greater, Start must be less than End",
                                    "/booking/create"
                            );
                        }
                ),
                Arguments.of(
                        "Required fields are null.",
                        null,
                        null,
                        null,
                        null,
                        null,
                        (MockServiceSetUp<CommandRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, _) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Date cannot be null, End cannot be null, Service Id cannot be null, Start cannot be null, User Id cannot be null",
                                    "/booking/create"
                            );
                        }
                ),
                Arguments.of(
                        "Internal Server Error, failed ot send message to Kafka.",
                        COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        COMMON_SERVICE_IDS.get(0).toHexString(),
                        COMMON_USER_IDS.get(0).toHexString(),
                        "900",
                        "990",
                        (MockServiceSetUp<CommandRemoteService>) (service, args) -> {
                            CreateBookingRequest request = new CreateBookingRequest(
                                    LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE),
                                    new ObjectId((String) args[1]),
                                    new ObjectId((String) args[2]),
                                    Integer.parseInt((String) args[3]),
                                    Integer.parseInt((String) args[4])
                            );
                            doThrow(new BookingCommandDeliveryException(null)).when(service).createBooking(request);
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, args) -> {
                            CreateBookingRequest request = new CreateBookingRequest(
                                    LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE),
                                    new ObjectId((String) args[1]),
                                    new ObjectId((String) args[2]),
                                    Integer.parseInt((String) args[3]),
                                    Integer.parseInt((String) args[4])
                            );
                            then(service).should().createBooking(request);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        (MockMvcExpectationAssertion) (resultActions) ->
                                assertProblemDetailExpectations(
                                        resultActions,
                                        "about:blank",
                                        "Internal Server Error",
                                        500,
                                        "Failed to deliver the booking command to Kafka",
                                        "/booking/create"
                                )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideCreateBookingTestCases")
    void createBooking(
            String name,
            String date,
            String serviceId,
            String userId,
            String start,
            String end,
            MockServiceSetUp<CommandRemoteService> mockServiceSetUp,
            MockServiceAssertion<CommandRemoteService> mockServiceAssertion,
            MockMvcExpectationAssertion mockMvcExpectationAssertion
    ) throws Exception {
        // given
        mockServiceSetUp.setUp(commandService, date, serviceId, userId, start, end);

        // when
        Map<String, Object> request = createBookingRequest(date, serviceId, userId, start, end);

        ResultActions resultActions = mockMvc.perform(
                post("/booking/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // then
        mockMvcExpectationAssertion.assertExpectations(resultActions);
        mockServiceAssertion.assertMock(commandService, date, serviceId, userId, start, end);
    }

    public static Map<String, Object> createBookingRequest(
            String date,
            String serviceId,
            String userId,
            String start,
            String end
    ) {
        Map<String, Object> request = new HashMap<>();
        request.put("date", date);
        request.put("serviceId", serviceId);
        request.put("userId", userId);
        request.put("start", start);
        request.put("end", end);
        return request;
    }

    private static Stream<Arguments> provideDeleteBookingTestCases() {
        return Stream.of(
                Arguments.of(
                        "OK response.",
                        COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        COMMON_SERVICE_IDS.get(0).toHexString(),
                        COMMON_BOOKING_IDS.get(0).toHexString(),
                        COMMON_USER_IDS.get(0).toHexString(),
                        (MockServiceSetUp<CommandRemoteService>) (service, args) -> {
                            DeleteBookingRequest request = new DeleteBookingRequest(
                                    LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE),
                                    new ObjectId((String) args[1]),
                                    new ObjectId((String) args[2]),
                                    new ObjectId((String) args[3])
                            );

                            doNothing().when(service).deleteBooking(request);
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, args) -> {
                            DeleteBookingRequest request = new DeleteBookingRequest(
                                    LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE),
                                    new ObjectId((String) args[1]),
                                    new ObjectId((String) args[2]),
                                    new ObjectId((String) args[3])
                            );
                            then(service).should().deleteBooking(request);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        (MockMvcExpectationAssertion) (resultActions) ->
                                resultActions.andExpect(status().isOk())
                ),
                Arguments.of(
                        "Incorrect date string format.",
                        "2024-12-32",
                        COMMON_SERVICE_IDS.get(0).toHexString(),
                        COMMON_BOOKING_IDS.get(0).toHexString(),
                        COMMON_USER_IDS.get(0).toHexString(),
                        (MockServiceSetUp<CommandRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, _) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "JSON parse error: Cannot deserialize value of type `java.time.LocalDate` from String \"2024-12-32\": Failed to deserialize java.time.LocalDate: (java.time.format.DateTimeParseException) Text '2024-12-32' could not be parsed: Invalid value for DayOfMonth (valid values 1 - 28/31): 32",
                                    "/booking/delete"
                            );
                        }
                ),
                Arguments.of(
                        "Required fields are null.",
                        null,
                        null,
                        null,
                        null,
                        (MockServiceSetUp<CommandRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, _) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Booking Id cannot be null, Date cannot be null, Service Id cannot be null, User Id cannot be null",
                                    "/booking/delete"
                            );
                        }
                ),
                Arguments.of(
                        "Internal Server Error, failed ot send message to Kafka.",
                        COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        COMMON_SERVICE_IDS.get(0).toHexString(),
                        COMMON_BOOKING_IDS.get(0).toHexString(),
                        COMMON_USER_IDS.get(0).toHexString(),
                        (MockServiceSetUp<CommandRemoteService>) (service, args) -> {
                            DeleteBookingRequest request = new DeleteBookingRequest(
                                    LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE),
                                    new ObjectId((String) args[1]),
                                    new ObjectId((String) args[2]),
                                    new ObjectId((String) args[3])
                            );
                            doThrow(new BookingCommandDeliveryException(null)).when(service).deleteBooking(request);
                        },
                        (MockServiceAssertion<CommandRemoteService>) (service, args) -> {
                            DeleteBookingRequest request = new DeleteBookingRequest(
                                    LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE),
                                    new ObjectId((String) args[1]),
                                    new ObjectId((String) args[2]),
                                    new ObjectId((String) args[3])
                            );
                            then(service).should().deleteBooking(request);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        (MockMvcExpectationAssertion) (resultActions) ->
                                assertProblemDetailExpectations(
                                        resultActions,
                                        "about:blank",
                                        "Internal Server Error",
                                        500,
                                        "Failed to deliver the booking command to Kafka",
                                        "/booking/delete"
                                )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideDeleteBookingTestCases")
    void deleteBooking(
            String name,
            String date,
            String serviceId,
            String bookingId,
            String userId,
            MockServiceSetUp<CommandRemoteService> mockServiceSetUp,
            MockServiceAssertion<CommandRemoteService> mockServiceAssertion,
            MockMvcExpectationAssertion mockMvcExpectationAssertion
    ) throws Exception {
        // given
        mockServiceSetUp.setUp(commandService, date, serviceId, bookingId, userId);

        // when
        Map<String, Object> request = deleteBookingRequest(date, serviceId, bookingId, userId);
        ResultActions resultActions = mockMvc.perform(
                post("/booking/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );

        // then
        mockMvcExpectationAssertion.assertExpectations(resultActions);
        mockServiceAssertion.assertMock(commandService, date, serviceId, bookingId, userId);
    }

    public static Map<String, Object> deleteBookingRequest(
            String date,
            String serviceId,
            String bookingId,
            String userId
    ) {
        Map<String, Object> request = new HashMap<>();
        request.put("date", date);
        request.put("serviceId", serviceId);
        request.put("bookingId", bookingId);
        request.put("userId", userId);
        return request;
    }

    private static Stream<Arguments> provideGetAvailableTimeRangesTestCases() {
        return Stream.of(
                Arguments.of(
                        "OK response.",
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).date(),
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).serviceId().toHexString(),
                        String.valueOf(75),
                        (MockServiceSetUp<QueryRemoteService>) (service, args) -> {
                            LocalDate date = LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE);
                            ObjectId serviceId = new ObjectId((String) args[1]);
                            int serviceDuration = Integer.parseInt((String) args[2]);

                            when(service.getAvailableTimeRanges(date, serviceId, serviceDuration))
                                    .thenReturn(COMMON_TIME_RANGE_RESPONSES);
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, args) -> {
                            LocalDate date = LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE);
                            ObjectId serviceId = new ObjectId((String) args[1]);
                            int serviceDuration = Integer.parseInt((String) args[2]);

                            then(service).should().getAvailableTimeRanges(date, serviceId, serviceDuration);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions
                                    .andExpect(status().isOk())
                                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                    .andExpect(jsonPath("$").isArray())
                                    .andExpect(jsonPath("$.length()").value(COMMON_TIME_RANGE_RESPONSES.size()));

                            for (int i = 0; i < COMMON_TIME_RANGE_RESPONSES.size(); i++) {
                                resultActions
                                        .andExpect(jsonPath("$[%d].start", i).value(COMMON_TIME_RANGE_RESPONSES.get(i).start()))
                                        .andExpect(jsonPath("$[%d].end", i).value(COMMON_TIME_RANGE_RESPONSES.get(i).end()))
                                        .andExpect(jsonPath(String.format("$[%d]", i), aMapWithSize(2)));
                            }
                        }
                ),
                Arguments.of(
                        "Invalid service duration number format.",
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).date(),
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).serviceId().toHexString(),
                        "invalid format",
                        (MockServiceSetUp<QueryRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, _) ->
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
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).date(),
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).serviceId().toHexString(),
                        String.valueOf(-1),
                        (MockServiceSetUp<QueryRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, _) ->
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
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).date(),
                        "Incorrect hex string",
                        String.valueOf(75),
                        (MockServiceSetUp<QueryRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, _) ->
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
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).serviceId().toHexString(),
                        String.valueOf(75),
                        (MockServiceSetUp<QueryRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, _) ->
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
            MockServiceSetUp<QueryRemoteService> mockServiceSetUp,
            MockServiceAssertion<QueryRemoteService> mockServiceAssertion,
            MockMvcExpectationAssertion mockMvcExpectationAssertion
    ) throws Exception {
        // given
        mockServiceSetUp.setUp(queryService, date, serviceId, serviceDuration);

        // when
        ResultActions resultActions = mockMvc.perform(get("/booking/available")
                .param("date", date)
                .param("serviceId", serviceId)
                .param("serviceDuration", serviceDuration)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        mockMvcExpectationAssertion.assertExpectations(resultActions);

        mockServiceAssertion.assertMock(queryService, date, serviceId, serviceDuration);
    }

    private static Stream<Arguments> provideGetUserBookingTestCases() {
        return Stream.of(
                Arguments.of(
                        "Ok response.",
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).date(),
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).serviceId().toHexString(),
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).bookings().get(0).id().toHexString(),
                        (MockServiceSetUp<QueryRemoteService>) (service, args) -> {
                            LocalDate date = LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE);
                            ObjectId serviceId = new ObjectId((String) args[1]);
                            ObjectId userId = new ObjectId((String) args[2]);

                            when(service.getUserBooking(date, serviceId, userId)).thenReturn(COMMON_USER_BOOKING_RESPONSE);
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, args) -> {
                            LocalDate date = LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE);
                            ObjectId serviceId = new ObjectId((String) args[1]);
                            ObjectId userId = new ObjectId((String) args[2]);

                            then(service).should().getUserBooking(date, serviceId, userId);
                            then(service).shouldHaveNoMoreInteractions();
                        },
                        (MockMvcExpectationAssertion) (resultActions) ->
                                resultActions
                                        .andExpect(status().isOk())
                                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                        .andExpect(jsonPath("$", aMapWithSize(3)))
                                        .andExpect(jsonPath("$.userId").value(COMMON_USER_BOOKING_RESPONSE.userId().toHexString()))
                                        .andExpect(jsonPath("$.start").value(String.valueOf(COMMON_USER_BOOKING_RESPONSE.start())))
                                        .andExpect(jsonPath("$.end").value(String.valueOf(COMMON_USER_BOOKING_RESPONSE.end())))
                ),
                Arguments.of(
                        "Incorrect date string format.",
                        "22004-10-33",
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).serviceId().toHexString(),
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).bookings().get(0).id().toHexString(),
                        (MockServiceSetUp<QueryRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, _) ->
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
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).date(),
                        "foo",
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).bookings().get(0).id().toHexString(),
                        (MockServiceSetUp<QueryRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, _) ->
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
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).date(),
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).serviceId().toHexString(),
                        "bar",
                        (MockServiceSetUp<QueryRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, _) ->
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
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).date(),
                        COMMON_SERVICE_BOOKING_RESPONSES.get(0).serviceId().toHexString(),
                        COMMON_SERVICE_BOOKING_RESPONSES.get(1).bookings().get(0).id().toHexString(),
                        (MockServiceSetUp<QueryRemoteService>) (service, args) -> {
                            LocalDate date = LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE);
                            ObjectId serviceId = new ObjectId((String) args[1]);
                            ObjectId userId = new ObjectId((String) args[2]);

                            String body = """
                                    {
                                       "type": "about:blank",
                                       "title": "Not Found",
                                       "status": 404,
                                       "detail": "UserBooking(date: 2024-12-12, serviceId: 010000000000000000000000, bookingId: 100000000000000000000002) was not found.",
                                       "instance": "/booking",
                                       "properties": null
                                    }""";

                            when(service.getUserBooking(date, serviceId, userId))
                                    .thenThrow(
                                            new FeignException.NotFound(
                                                    "",
                                                    Request.create(Request.HttpMethod.GET, "/booking", Collections.emptyMap(), null, StandardCharsets.UTF_8, null),
                                                    body.getBytes(StandardCharsets.UTF_8),
                                                    Collections.emptyMap()
                                            )
                                    );
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, args) -> {
                            LocalDate date = LocalDate.parse((String) args[0], DateTimeFormatter.ISO_LOCAL_DATE);
                            ObjectId serviceId = new ObjectId((String) args[1]);
                            ObjectId userId = new ObjectId((String) args[2]);

                            then(service).should().getUserBooking(date, serviceId, userId);
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
            MockServiceSetUp<QueryRemoteService> mockServiceSetUp,
            MockServiceAssertion<QueryRemoteService> mockServiceAssertion,
            MockMvcExpectationAssertion mockMvcExpectationAssertion
    ) throws Exception {
        // given
        mockServiceSetUp.setUp(queryService, date, serviceId, bookingId);

        // when
        ResultActions resultActions = mockMvc.perform(get("/booking")
                .param("date", date)
                .param("serviceId", serviceId)
                .param("bookingId", bookingId)
                .contentType(MediaType.APPLICATION_JSON));

        // then
        mockMvcExpectationAssertion.assertExpectations(resultActions);

        mockServiceAssertion.assertMock(queryService, date, serviceId, bookingId);
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
                ),
                Arguments.of(
                        "Incorrect date string format.",
                        toStringParam(COMMON_DATES) + ",foo",
                        null,
                        null,
                        (MockServiceSetUp<QueryRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, _) ->
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
                        toStringParam(COMMON_SERVICE_IDS) + ",foo",
                        null,
                        (MockServiceSetUp<QueryRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, _) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Invalid ObjectId: 010000000000000000000000,010000000000000000000001,foo",
                                    "/booking/all"
                            );
                        }
                ),
                Arguments.of(
                        "Incorrect hex string for userId.",
                        null,
                        null,
                        toStringParam(COMMON_USER_IDS) + ",foo",
                        (MockServiceSetUp<QueryRemoteService>) (_, _) -> {
                        },
                        (MockServiceAssertion<QueryRemoteService>) (service, _) ->
                                then(service).shouldHaveNoInteractions(),
                        (MockMvcExpectationAssertion) (resultActions) -> {
                            resultActions.andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
                            assertProblemDetailExpectations(
                                    resultActions,
                                    "about:blank",
                                    "Bad Request",
                                    400,
                                    "Invalid ObjectId: 110000000000000000000000,110000000000000000000001,foo",
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

    private static final List<TimeRangeResponse> COMMON_TIME_RANGE_RESPONSES = List.of(
            new TimeRangeResponse(900, 990),
            new TimeRangeResponse(990, 1080)
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

    private static final List<Booking> COMMON_BOOKINGS = List.of(
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

    private static final UserBookingResponse COMMON_USER_BOOKING_RESPONSE = new UserBookingResponse(
            COMMON_BOOKINGS.get(0).id(),
            COMMON_BOOKINGS.get(0).start(),
            COMMON_BOOKINGS.get(0).end()
    );

    private static final List<ServiceBookingResponse> COMMON_SERVICE_BOOKING_RESPONSES = List.of(
            new ServiceBookingResponse(
                    COMMON_SERVICE_BOOKING_IDS.get(0),
                    COMMON_DATES.get(0).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    COMMON_SERVICE_IDS.get(0),
                    List.of(
                            COMMON_BOOKINGS.get(0),
                            COMMON_BOOKINGS.get(1)
                    )
            ),
            new ServiceBookingResponse(
                    COMMON_SERVICE_BOOKING_IDS.get(1),
                    COMMON_DATES.get(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    COMMON_SERVICE_IDS.get(1),
                    List.of(
                            COMMON_BOOKINGS.get(2),
                            COMMON_BOOKINGS.get(3)
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