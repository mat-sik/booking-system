package com.github.matsik.query.booking.query;

import com.github.matsik.cassandra.model.BookingPartitionKey;

import java.time.LocalDate;
import java.util.UUID;

public record GetUserBookingQuery(BookingPartitionKey bookingPartitionKey, UUID userId, UUID bookingId) {
    public static class Factory {
        public static GetUserBookingQuery create(LocalDate date, UUID serviceId, UUID userId, UUID bookingId) {
            BookingPartitionKey identifier = BookingPartitionKey.Factory.create(date, serviceId);
            return new GetUserBookingQuery(identifier, userId, bookingId);
        }
    }
}
