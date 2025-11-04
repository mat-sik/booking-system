package com.github.matsik.query.booking.query;

import com.github.matsik.dto.BookingPartitionKey;

import java.time.LocalDate;
import java.util.UUID;

public record GetUserBookingQuery(BookingPartitionKey bookingPartitionKey, UUID userId, UUID bookingId) {
    public static GetUserBookingQuery of(UUID serviceId, LocalDate date, UUID userId, UUID bookingId) {
        BookingPartitionKey key = BookingPartitionKey.of(serviceId, date);
        return new GetUserBookingQuery(key, userId, bookingId);
    }
}
