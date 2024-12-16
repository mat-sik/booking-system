package com.github.matsik.query.booking.controller;

import com.github.matsik.query.booking.model.ServiceBooking;
import com.github.matsik.query.booking.model.UserBooking;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingQuery;
import com.github.matsik.query.booking.query.GetBookingsQuery;
import com.github.matsik.query.booking.service.BookingService;
import com.github.matsik.query.booking.service.TimeRange;
import com.github.matsik.query.response.ServiceBookingResponse;
import com.github.matsik.query.response.TimeRangeResponse;
import com.github.matsik.query.response.UserBookingResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
@Validated
public class BookingController {

    private final BookingService service;

    @GetMapping("/available")
    public ResponseEntity<List<TimeRangeResponse>> getAvailableTimeRanges(
            @RequestParam LocalDate date,
            @RequestParam ObjectId serviceId,
            @RequestParam @Positive int serviceDuration
    ) {
        GetAvailableTimeRangesQuery query = GetAvailableTimeRangesQuery.Factory.create(date, serviceId, serviceDuration);
        List<TimeRange> availableTimeRanges = service.getAvailableTimeRanges(query);

        List<TimeRangeResponse> response = availableTimeRanges.stream()
                .map(model -> mapToResponse(model))
                .toList();

        return ResponseEntity.ok(response);
    }

    private static TimeRangeResponse mapToResponse(TimeRange model) {
        return new TimeRangeResponse(
                model.start(),
                model.end()
        );
    }

    @GetMapping
    public ResponseEntity<UserBookingResponse> getUserBooking(
            @RequestParam LocalDate date,
            @RequestParam ObjectId serviceId,
            @RequestParam ObjectId bookingId
    ) {
        GetBookingQuery query = GetBookingQuery.Factory.create(date, serviceId, bookingId);
        UserBooking userBooking = service.getUserBooking(query);

        UserBookingResponse response = mapToResponse(userBooking);

        return ResponseEntity.ok(response);
    }

    private static UserBookingResponse mapToResponse(UserBooking model) {
        return new UserBookingResponse(
                model.userId(),
                model.start(),
                model.end()
        );
    }

   @GetMapping("/all")
    public ResponseEntity<List<ServiceBookingResponse>> getBookings(
            @RequestParam(required = false, defaultValue = "") List<LocalDate> dates,
            @RequestParam(required = false, defaultValue = "") List<ObjectId> serviceIds,
            @RequestParam(required = false, defaultValue = "") List<ObjectId> userIds
    ) {
        GetBookingsQuery query = new GetBookingsQuery(dates, serviceIds, userIds);
        List<ServiceBooking> serviceBookings = service.getBookings(query);

        List<ServiceBookingResponse> response = serviceBookings.stream()
                .map(model -> mapToResponse(model))
                .toList();

        return ResponseEntity.ok(response);
    }

    private ServiceBookingResponse mapToResponse(ServiceBooking model) {
        return new ServiceBookingResponse(
                model.id(),
                model.date(),
                model.serviceId(),
                model.bookings()
        );
    }
}
