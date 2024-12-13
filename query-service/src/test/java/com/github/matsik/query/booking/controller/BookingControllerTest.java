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
import java.util.stream.Stream;

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
                getArguments(
                        "OK response.",
                        LocalDate.of(2024, 12, 12).format(DateTimeFormatter.ISO_LOCAL_DATE),
                        new ObjectId("000000000000000000000000").toHexString(),
                        String.valueOf(75),
                        List.of(
                                new TimeRange(900, 990),
                                new TimeRange(990, 1080)
                        ),
                        (resultActions, availableTimeRanges) -> {
                            resultActions.andExpect(status().isOk())
                                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                    .andExpect(jsonPath("$").isArray());

                            for (int i = 0; i < availableTimeRanges.size(); i++) {
                                resultActions
                                        .andExpect(jsonPath("$[%d].start", i).value(availableTimeRanges.get(i).start()))
                                        .andExpect(jsonPath("$[%d].end", i).value(availableTimeRanges.get(i).end()));
                            }
                        }
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetAvailableTimeRangesTestCases")
    void getAvailableTimeRanges(
            String name,
            GetAvailableTimeRangesQuery query,
            List<TimeRange> availableTimeRanges,
            Runnable assertMvc
    ) {
        // given
        when(SERVICE.getAvailableTimeRanges(eq(query)))
                .thenReturn(availableTimeRanges);

        // when && then
        assertMvc.run();
    }

    private static Arguments getArguments(
            String name,
            String date,
            String serviceId,
            String serviceDuration,
            List<TimeRange> availableTimeRanges,
            MvcAssertExpectations mvcAssertExpectations
    ) {
        GetAvailableTimeRangesQuery query = getGetAvailableTimeRangesQueryOrDefault(date, serviceId, serviceDuration);

        Runnable assertMvc = () -> {
            try {
                ResultActions resultActions = MOCK_MVC.perform(get("/booking/available")
                        .param("date", date)
                        .param("serviceId", serviceId)
                        .param("serviceDuration", serviceDuration)
                        .contentType(MediaType.APPLICATION_JSON));

                mvcAssertExpectations.expect(resultActions, availableTimeRanges);

                then(SERVICE).should().getAvailableTimeRanges(query);
                then(SERVICE).shouldHaveNoMoreInteractions();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };

        return Arguments.of(
                name,
                query,
                availableTimeRanges,
                assertMvc
        );
    }

    @FunctionalInterface
    private interface MvcAssertExpectations {
        void expect(ResultActions resultActions, List<TimeRange> availableTimeRanges) throws Exception;
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