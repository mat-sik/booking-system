package com.github.matsik.booking.controller.response;

import com.github.matsik.cassandra.entity.BookingByServiceAndDate;

import java.util.List;
import java.util.UUID;

public record ServiceBookingResponse(
        UUID id,
        String date,
        UUID serviceId,
        List<BookingByServiceAndDate> bookings
) {
}
