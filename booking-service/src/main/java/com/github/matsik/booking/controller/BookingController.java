package com.github.matsik.booking.controller;

import com.github.matsik.booking.client.command.CommandClient;
import com.github.matsik.booking.client.query.QueryClient;
import com.github.matsik.booking.controller.request.CreateBookingRequest;
import com.github.matsik.booking.controller.request.DeleteBookingRequest;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
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

    private final QueryClient queryClient;
    private final CommandClient commandClient;

    @PostMapping("/create")
    public void createBooking(@RequestBody CreateBookingRequest request) {
        LocalDate key = request.date();

        CreateBookingCommandValue value = new CreateBookingCommandValue(
                request.serviceId(),
                request.userId(),
                request.start(),
                request.end()
        );

        commandClient.sendCreateBookingCommand(key, value);
    }

    @PostMapping("/delete")
    public void deleteBooking(@RequestBody DeleteBookingRequest request) {
        LocalDate key = request.date();

        DeleteBookingCommandValue value = new DeleteBookingCommandValue(
                request.serviceId(),
                request.bookingId()
        );

        commandClient.sendDeleteBookingCommand(key, value);
    }

    @GetMapping("/available")
    public ResponseEntity<List<TimeRangeResponse>> getAvailableTimeRanges(
            @RequestParam("date") LocalDate date,
            @RequestParam("serviceId") ObjectId serviceId,
            @RequestParam("serviceDuration") int serviceDuration
    ) {

        return queryClient.getAvailableTimeRanges(date, serviceId, serviceDuration);
    }

    @GetMapping
    public ResponseEntity<UserBookingResponse> getUserBooking(
            @RequestParam("date") LocalDate date,
            @RequestParam("serviceId") ObjectId serviceId,
            @RequestParam("bookingId") ObjectId bookingId
    ) {
        return queryClient.getUserBooking(date, serviceId, bookingId);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ServiceBookingResponse>> getBookings(
            @RequestParam("dates") List<LocalDate> dates,
            @RequestParam("serviceIds") List<ObjectId> serviceIds,
            @RequestParam("userIds") List<ObjectId> userIds
    ) {
        return queryClient.getBookings(dates, serviceIds, userIds);
    }
}
