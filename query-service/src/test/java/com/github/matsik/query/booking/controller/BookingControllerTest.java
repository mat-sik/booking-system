package com.github.matsik.query.booking.controller;

import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.service.BookingService;
import com.github.matsik.query.booking.service.TimeRange;
import org.bson.types.ObjectId;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
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
                        (MockServiceAssertion) (service, query) -> {
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
                            resultActions
                                    .andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                                    .andExpect(jsonPath("$", aMapWithSize(5)))
                                    .andExpect(jsonPath("$.type").value("about:blank"))
                                    .andExpect(jsonPath("$.title").value("Bad Request"))
                                    .andExpect(jsonPath("$.status").value(400))
                                    .andExpect(jsonPath("$.detail").value("For input string: \"invalidformat\""))
                                    .andExpect(jsonPath("$.instance").value("/booking/available"));
                        },
                        (MockServiceAssertion) (service, query) -> then(service).shouldHaveNoMoreInteractions()
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
                            resultActions
                                    .andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                                    .andExpect(jsonPath("$", aMapWithSize(5)))
                                    .andExpect(jsonPath("$.type").value("about:blank"))
                                    .andExpect(jsonPath("$.title").value("Bad Request"))
                                    .andExpect(jsonPath("$.status").value(400))
                                    .andExpect(jsonPath("$.detail").value("getAvailableTimeRanges.serviceDuration: must be greater than 0"))
                                    .andExpect(jsonPath("$.instance").value("/booking/available"));
                        },
                        (MockServiceAssertion) (service, query) -> then(service).shouldHaveNoMoreInteractions()
                ),
                Arguments.of(
                        "Incorrect hex string for serviceId.",
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        "Incorrect hex string",
                        String.valueOf(75),
                        List.of(
                                new TimeRange(900, 990),
                                new TimeRange(990, 1080)
                        ),
                        (MockMvcExpectationAssertion<List<TimeRange>>) (resultActions, availableTimeRanges) -> {
                            resultActions
                                    .andExpect(status().isBadRequest())
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                                    .andExpect(jsonPath("$", aMapWithSize(5)))
                                    .andExpect(jsonPath("$.type").value("about:blank"))
                                    .andExpect(jsonPath("$.title").value("Bad Request"))
                                    .andExpect(jsonPath("$.status").value(400))
                                    .andExpect(jsonPath("$.detail").value("Invalid ObjectId: Incorrect hex string"))
                                    .andExpect(jsonPath("$.instance").value("/booking/available"));
                        },
                        (MockServiceAssertion) (service, query) -> then(service).shouldHaveNoMoreInteractions()
                ),
                Arguments.of(
                        "Incorrect date.",
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
                                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                                    .andExpect(jsonPath("$", aMapWithSize(5)))
                                    .andExpect(jsonPath("$.type").value("about:blank"))
                                    .andExpect(jsonPath("$.title").value("Bad Request"))
                                    .andExpect(jsonPath("$.status").value(400))
                                    .andExpect(jsonPath("$.detail").value("Parse attempt failed for value [22004-10-33]"))
                                    .andExpect(jsonPath("$.instance").value("/booking/available"));
                        },
                        (MockServiceAssertion) (service, query) -> then(service).shouldHaveNoMoreInteractions()
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
            MockServiceAssertion mockServiceAssertion
    ) throws Exception {
        // given
        GetAvailableTimeRangesQuery query = getGetAvailableTimeRangesQueryOrDefault(date, serviceId, serviceDuration);
        when(service.getAvailableTimeRanges(eq(query)))
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

    private interface MockServiceAssertion {
        void assertMock(BookingService service, GetAvailableTimeRangesQuery query);
    }

    @FunctionalInterface
    private interface MockMvcExpectationAssertion<T> {
        void assertExpectations(ResultActions resultActions, T expectation) throws Exception;
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