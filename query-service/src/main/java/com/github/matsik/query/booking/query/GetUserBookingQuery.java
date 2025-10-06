package com.github.matsik.query.booking.query;

import com.github.matsik.cassandra.model.BookingPartitionKey;

import java.time.LocalDate;
import java.util.UUID;

public record GetUserBookingQuery(BookingPartitionKey bookingPartitionKey, UUID userId, UUID bookingId) {
    public static class Factory {
        public static GetUserBookingQuery create(UUID serviceId, LocalDate date, UUID userId, UUID bookingId) {
            BookingPartitionKey key = BookingPartitionKey.Factory.create(serviceId, date);
            return new GetUserBookingQuery(key, userId, bookingId);
        }
    }
}
