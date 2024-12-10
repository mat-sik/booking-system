package com.github.matsik.booking.controller;

import com.github.matsik.booking.client.command.CommandRemoteService;
import com.github.matsik.booking.client.query.QueryRemoteService;
import com.github.matsik.booking.controller.request.CreateBookingRequest;
import com.github.matsik.booking.controller.request.DeleteBookingRequest;
import com.github.matsik.query.response.ServiceBookingResponse;
import com.github.matsik.query.response.TimeRangeResponse;
import com.github.matsik.query.response.UserBookingResponse;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
public class BookingController {

    private final QueryRemoteService queryService;
    private final CommandRemoteService commandService;

    @PostMapping("/create")
    public void createBooking(@RequestBody CreateBookingRequest request) {
        commandService.createBooking(request);
    }

    @PostMapping("/delete")
    public void deleteBooking(@RequestBody DeleteBookingRequest request) {
        commandService.deleteBooking(request);
    }

    @GetMapping("/available")
    public ResponseEntity<List<TimeRangeResponse>> getAvailableTimeRanges(
            @RequestParam("date") LocalDate date,
            @RequestParam("serviceId") ObjectId serviceId,
            @RequestParam("serviceDuration") int serviceDuration
    ) {
        List<TimeRangeResponse> responses = queryService.getAvailableTimeRanges(date, serviceId, serviceDuration);
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    public ResponseEntity<UserBookingResponse> getUserBooking(
            @RequestParam("date") LocalDate date,
            @RequestParam("serviceId") ObjectId serviceId,
            @RequestParam("bookingId") ObjectId bookingId
    ) {
        UserBookingResponse response = queryService.getUserBooking(date, serviceId, bookingId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ServiceBookingResponse>> getBookings(
            @RequestParam(required = false, defaultValue = "") List<LocalDate> dates,
            @RequestParam(required = false, defaultValue = "") List<ObjectId> serviceIds,
            @RequestParam(required = false, defaultValue = "") List<ObjectId> userIds
    ) {
        List<ServiceBookingResponse> responses = queryService.getBookings(dates, serviceIds, userIds);
        return ResponseEntity.ok(responses);
    }
}
