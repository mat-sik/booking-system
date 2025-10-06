package com.github.matsik.query.booking.controller;

import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetFirstUserBookingsQuery;
import com.github.matsik.query.booking.query.GetNextUserBookingsQuery;
import com.github.matsik.query.booking.query.GetUserBookingQuery;
import com.github.matsik.query.booking.query.GetUserBookingsQuery;
import com.github.matsik.query.booking.repository.projection.TimeRange;
import com.github.matsik.query.booking.repository.projection.UserBooking;
import com.github.matsik.query.booking.service.BookingService;
import com.github.matsik.query.response.TimeRangeResponse;
import com.github.matsik.query.response.UserBookingResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
@Validated
public class BookingController {

    private final BookingService service;

    @GetMapping("/available")
    public ResponseEntity<List<TimeRangeResponse>> getAvailableTimeRanges(
            @RequestParam UUID serviceId,
            @RequestParam LocalDate date,
            @RequestParam @Positive int serviceDuration
    ) {
        GetAvailableTimeRangesQuery query = GetAvailableTimeRangesQuery.Factory.create(serviceId, date, serviceDuration);
        List<TimeRange> availableTimeRanges = service.getAvailableTimeRanges(query);

        List<TimeRangeResponse> response = availableTimeRanges.stream()
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<TimeRangeResponse> getUserBookingTimeRange(
            @RequestParam UUID serviceId,
            @RequestParam LocalDate date,
            @RequestParam UUID userId,
            @RequestParam UUID bookingId
    ) {
        GetUserBookingQuery query = GetUserBookingQuery.Factory.create(serviceId, date, userId, bookingId);

        TimeRange userBookingTimeRange = service.getUserBookingTimeRange(query);

        return ResponseEntity.ok(mapToResponse(userBookingTimeRange));
    }

    private TimeRangeResponse mapToResponse(TimeRange model) {
        return new TimeRangeResponse(
                model.start(),
                model.end()
        );
    }

    @GetMapping("/many")
    public ResponseEntity<List<UserBookingResponse>> getUserBookings(
            @RequestParam UUID userId,
            @RequestParam(required = false) UUID cursorServiceId,
            @RequestParam(required = false) LocalDate cursorDate,
            @RequestParam(required = false) UUID cursorBookingId,
            @RequestParam int limit
    ) {
        GetUserBookingsQuery query = query(userId, cursorServiceId, cursorDate, cursorBookingId, limit);

        List<UserBooking> userBookings = service.getUserBookings(query);

        List<UserBookingResponse> response = userBookings.stream()
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    private GetUserBookingsQuery query(
            UUID userId,
            UUID cursorServiceId,
            LocalDate cursorDate,
            UUID cursorBookingId,
            int limit
    ) {
        if (cursorServiceId == null) {
            return new GetFirstUserBookingsQuery(userId, limit);
        }
        return new GetNextUserBookingsQuery(userId, cursorServiceId, cursorDate, cursorBookingId, limit);
    }

    private UserBookingResponse mapToResponse(UserBooking userBooking) {
        return new UserBookingResponse(
                userBooking.serviceId(),
                userBooking.date(),
                userBooking.bookingId(),
                userBooking.start(),
                userBooking.end()
        );
    }
}
