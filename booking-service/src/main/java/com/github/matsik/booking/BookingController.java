package com.github.matsik.booking;

import com.github.matsik.booking.query.QueryClient;
import com.github.matsik.query.response.ServiceBookingResponse;
import com.github.matsik.query.response.TimeRangeResponse;
import com.github.matsik.query.response.UserBookingResponse;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
