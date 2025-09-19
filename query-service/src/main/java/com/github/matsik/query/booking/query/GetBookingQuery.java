package com.github.matsik.query.booking.query;

import com.github.matsik.cassandra.model.BookingPartitionKey;
import org.bson.types.ObjectId;

import java.time.LocalDate;

public record GetBookingQuery(BookingPartitionKey bookingPartitionKey, ObjectId bookingId) {
    public static class Factory {
        public static GetBookingQuery create(LocalDate date, ObjectId serviceId, ObjectId bookingId) {
            BookingPartitionKey identifier = BookingPartitionKey.Factory.create(date, serviceId);
            return new GetBookingQuery(identifier, bookingId);
        }
    }
}
