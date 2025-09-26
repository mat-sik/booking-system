package com.github.matsik.query.response;

import com.github.matsik.cassandra.model.BookingByServiceAndDate;

import java.util.List;
import java.util.UUID;

public record ServiceBookingResponse(
        UUID id,
        String date,
        UUID serviceId,
        List<BookingByServiceAndDate> bookings
) {
}
