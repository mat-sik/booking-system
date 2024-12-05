package com.github.matsik.query.booking.controller;

import com.github.matsik.query.booking.model.ServiceBooking;
import com.github.matsik.query.booking.model.UserBooking;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingQuery;
import com.github.matsik.query.booking.query.GetBookingTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingsQuery;
import com.github.matsik.query.booking.service.BookingService;
import com.github.matsik.query.booking.service.TimeRange;
import com.github.matsik.query.request.GetAvailableTimeRangesRequest;
import com.github.matsik.query.request.GetBookingRequest;
import com.github.matsik.query.request.GetBookingsRequest;
import com.github.matsik.query.response.ServiceBookingResponse;
import com.github.matsik.query.response.TimeRangeResponse;
import com.github.matsik.query.response.UserBookingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService service;

    @GetMapping("/available")
    public ResponseEntity<List<TimeRangeResponse>> getAvailableTimeRanges(
            @RequestParam String date,
            @RequestParam String serviceId,
            @RequestParam int serviceDuration
    ) {
        GetAvailableTimeRangesRequest request = GetAvailableTimeRangesRequest.Factory.create(date, serviceId, serviceDuration);

        GetAvailableTimeRangesQuery query = mapToQuery(request);
        List<TimeRange> availableTimeRanges = service.getAvailableTimeRanges(query);

        List<TimeRangeResponse> response = availableTimeRanges.stream()
                .map(model -> mapToResponse(model))
                .toList();

        return ResponseEntity.ok(response);
    }

    private static GetAvailableTimeRangesQuery mapToQuery(GetAvailableTimeRangesRequest request) {
        return new GetAvailableTimeRangesQuery(
                new GetBookingTimeRangesQuery(request.serviceBookingIdentifier()),
                request.serviceDuration()
        );
    }

    private static TimeRangeResponse mapToResponse(TimeRange model) {
        return new TimeRangeResponse(
                model.start(),
                model.end()
        );
    }

    @GetMapping
    public ResponseEntity<UserBookingResponse> getUserBooking(
            @RequestParam String date,
            @RequestParam String serviceId,
            @RequestParam String bookingId
    ) {
        GetBookingRequest request = GetBookingRequest.Factory.create(date, serviceId, bookingId);

        GetBookingQuery query = mapToQuery(request);
        UserBooking userBooking = service.getUserBooking(query);

        UserBookingResponse response = mapToResponse(userBooking);

        return ResponseEntity.ok(response);
    }

    private static GetBookingQuery mapToQuery(GetBookingRequest request) {
        return new GetBookingQuery(request.serviceBookingIdentifier(), request.bookingId());
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
            @RequestParam List<String> dates,
            @RequestParam List<String> serviceIds,
            @RequestParam List<String> userIds
    ) {
        GetBookingsRequest request = GetBookingsRequest.Factory.create(dates, serviceIds, userIds);

        GetBookingsQuery query = mapToQuery(request);
        List<ServiceBooking> serviceBookings = service.getBookings(query);

        List<ServiceBookingResponse> response = serviceBookings.stream()
                .map(model -> mapToResponse(model))
                .toList();

        return ResponseEntity.ok(response);
    }

    private static GetBookingsQuery mapToQuery(GetBookingsRequest request) {
        return new GetBookingsQuery(
                request.dates(),
                request.serviceIds(),
                request.userIds()
        );
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
