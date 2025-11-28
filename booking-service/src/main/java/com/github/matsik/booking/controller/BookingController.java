package com.github.matsik.booking.controller;

import com.github.matsik.booking.client.command.CommandRemoteService;
import com.github.matsik.booking.client.query.QueryRemoteService;
import com.github.matsik.booking.controller.request.CreateBookingRequest;
import com.github.matsik.booking.controller.request.DeleteBookingRequest;
import com.github.matsik.booking.controller.response.TimeRangeResponse;
import com.github.matsik.booking.controller.response.UserBookingResponse;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
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

import static com.github.matsik.booking.metrics.MetricsRecorder.recordMetrics;

@RestController
@RequestMapping("/bookings")
@Validated
@RequiredArgsConstructor
public class BookingController {

    private final QueryRemoteService queryService;
    private final CommandRemoteService commandService;

    private final LongCounter requestCounter;
    private final DoubleHistogram requestHistogram;

    @PostMapping("/create")
    public void createBooking(@RequestBody @Valid CreateBookingRequest request) {
        recordMetrics(requestCounter, requestHistogram, () -> {
            commandService.createBooking(request);
            return null;
        }, "create");
    }

    @PostMapping("/delete")
    public void deleteBooking(@RequestBody @Valid DeleteBookingRequest request) {
        recordMetrics(requestCounter, requestHistogram, () -> {
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
        return recordMetrics(requestCounter, requestHistogram,
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
        return recordMetrics(requestCounter, requestHistogram,
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
        return recordMetrics(requestCounter, requestHistogram, () -> {
            if (cursorServiceId != null && cursorDate != null && cursorBookingId != null) {
                return ResponseEntity.ok(queryService.getNextUserBookings(userId, cursorServiceId, cursorDate, cursorBookingId, limit));
            }
            return ResponseEntity.ok(queryService.getFirstUserBookings(userId, limit));
        }, "get_user_bookings");
    }

}
