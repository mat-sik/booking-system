package com.github.matsik.booking.client.query;

import com.github.matsik.query.response.ServiceBookingResponse;
import com.github.matsik.query.response.TimeRangeResponse;
import com.github.matsik.query.response.UserBookingResponse;
import org.bson.types.ObjectId;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Component
@FeignClient(name = "queryClient", url = "${query.service.url}")
public interface QueryClient {
    @GetMapping("/booking/available")
    List<TimeRangeResponse> getAvailableTimeRanges(
            @RequestParam("date") LocalDate date,
            @RequestParam("serviceId") ObjectId serviceId,
            @RequestParam("serviceDuration") int serviceDuration
    );

    @GetMapping("/booking")
    UserBookingResponse getUserBooking(
            @RequestParam("date") LocalDate date,
            @RequestParam("serviceId") ObjectId serviceId,
            @RequestParam("bookingId") ObjectId bookingId
    );

    @GetMapping("/booking/all")
    List<ServiceBookingResponse> getBookings(
            @RequestParam("dates") List<LocalDate> dates,
            @RequestParam("serviceIds") List<ObjectId> serviceIds,
            @RequestParam("userIds") List<ObjectId> userIds
    );
}
