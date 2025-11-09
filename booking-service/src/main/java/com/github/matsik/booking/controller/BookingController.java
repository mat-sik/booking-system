package com.github.matsik.booking.controller;

import com.github.matsik.booking.client.command.CommandRemoteService;
import com.github.matsik.booking.client.query.QueryRemoteService;
import com.github.matsik.booking.controller.request.CreateBookingRequest;
import com.github.matsik.booking.controller.request.DeleteBookingRequest;
import com.github.matsik.booking.controller.response.TimeRangeResponse;
import com.github.matsik.booking.controller.response.UserBookingResponse;
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

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Validated
public class BookingController {

    private final QueryRemoteService queryService;
    private final CommandRemoteService commandService;

    @PostMapping("/create")
    public void createBooking(@RequestBody @Valid CreateBookingRequest request) {
        commandService.createBooking(request);
    }

    @PostMapping("/delete")
    public void deleteBooking(@RequestBody @Valid DeleteBookingRequest request) {
        commandService.deleteBooking(request);
    }

    @GetMapping("/available")
    public ResponseEntity<List<TimeRangeResponse>> getAvailableTimeRanges(
            @RequestParam UUID serviceId,
            @RequestParam LocalDate date,
            @RequestParam @Positive int serviceDuration
    ) {
        return ResponseEntity.ok(queryService.getAvailableTimeRanges(serviceId, date, serviceDuration));
    }

    @GetMapping("/user")
    public ResponseEntity<TimeRangeResponse> getUserBookingTimeRange(
            @RequestParam UUID serviceId,
            @RequestParam LocalDate date,
            @RequestParam UUID userId,
            @RequestParam UUID bookingId
    ) {
        return ResponseEntity.ok(queryService.getUserBookingTimeRange(serviceId, date, userId, bookingId));
    }

    @GetMapping
    public ResponseEntity<List<UserBookingResponse>> getUserBookings(
            @RequestParam UUID userId,
            @RequestParam(required = false) UUID cursorServiceId,
            @RequestParam(required = false) LocalDate cursorDate,
            @RequestParam(required = false) UUID cursorBookingId,
            @RequestParam int limit
    ) {
        if (cursorServiceId != null && cursorDate != null && cursorBookingId != null) {
            return ResponseEntity.ok(queryService.getNextUserBookings(userId, cursorServiceId, cursorDate, cursorBookingId, limit));
        }
        return ResponseEntity.ok(queryService.getFirstUserBookings(userId, limit));
    }

}
