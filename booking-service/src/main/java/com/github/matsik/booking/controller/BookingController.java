package com.github.matsik.booking.controller;

import com.github.matsik.booking.client.command.CommandRemoteService;
import com.github.matsik.booking.client.query.QueryRemoteService;
import com.github.matsik.booking.controller.request.CreateBookingRequest;
import com.github.matsik.booking.controller.request.DeleteBookingRequest;
import com.github.matsik.booking.controller.response.TimeRangeResponse;
import com.github.matsik.booking.controller.response.UserBookingResponse;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Validated
public class BookingController {

    private final QueryRemoteService queryService;
    private final CommandRemoteService commandService;
    private final Meter meter;

    @PostMapping("/create")
    public void createBooking(@RequestBody @Valid CreateBookingRequest request) {
        recordMetrics(() -> {
            commandService.createBooking(request);
            return null;
        }, "create");
    }

    @PostMapping("/delete")
    public void deleteBooking(@RequestBody @Valid DeleteBookingRequest request) {
        recordMetrics(() -> {
            commandService.deleteBooking(request);
            return null;
        }, "delete");
    }

    @GetMapping("/available")
    public ResponseEntity<List<TimeRangeResponse>> getAvailableTimeRanges(
            @RequestParam UUID serviceId,
            @RequestParam LocalDate date,
            @RequestParam @Positive int serviceDuration
    ) {
        return recordMetrics(
                () -> ResponseEntity.ok(queryService.getAvailableTimeRanges(serviceId, date, serviceDuration)),
                "get_available_time_ranges"
        );
    }

    @GetMapping("/user")
    public ResponseEntity<TimeRangeResponse> getUserBookingTimeRange(
            @RequestParam UUID serviceId,
            @RequestParam LocalDate date,
            @RequestParam UUID userId,
            @RequestParam UUID bookingId
    ) {
        return recordMetrics(
                () -> ResponseEntity.ok(queryService.getUserBookingTimeRange(serviceId, date, userId, bookingId)),
                "get_user_booking_time_range"
        );
    }

    @GetMapping
    public ResponseEntity<List<UserBookingResponse>> getUserBookings(
            @RequestParam UUID userId,
            @RequestParam(required = false) UUID cursorServiceId,
            @RequestParam(required = false) LocalDate cursorDate,
            @RequestParam(required = false) UUID cursorBookingId,
            @RequestParam int limit
    ) {
        return recordMetrics(() -> {
            if (cursorServiceId != null && cursorDate != null && cursorBookingId != null) {
                return ResponseEntity.ok(queryService.getNextUserBookings(userId, cursorServiceId, cursorDate, cursorBookingId, limit));
            }
            return ResponseEntity.ok(queryService.getFirstUserBookings(userId, limit));
        }, "get_user_bookings");
    }

    private <T> T recordMetrics(Supplier<T> operation, String operationName) {
        long startTime = System.nanoTime();
        T result = operation.get();
        long duration = System.nanoTime() - startTime;

        recordDurationAndIncrementCounter(meter, duration, operationName);

        return result;
    }

    private void recordDurationAndIncrementCounter(Meter meter, long duration, String requestName) {
        LongCounter counter = meter.counterBuilder(String.format("%s.requests", requestName))
                .setDescription(String.format("Total %s requests", requestName))
                .setUnit("requests")
                .build();

        DoubleHistogram histogram = meter.histogramBuilder(String.format("%s.duration", requestName))
                .setDescription(String.format("Duration of handling a %s request", requestName))
                .setUnit("ms")
                .build();

        counter.add(1L);
        histogram.record(duration);
    }

}
