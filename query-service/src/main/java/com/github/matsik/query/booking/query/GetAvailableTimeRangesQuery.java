package com.github.matsik.query.booking.query;

import com.github.matsik.cassandra.model.BookingPartitionKey;
import org.bson.types.ObjectId;

import java.time.LocalDate;

public record GetAvailableTimeRangesQuery(GetBookingTimeRangesQuery getBookingTimeRangesQuery, int serviceDuration) {
    public static class Factory {
        public static GetAvailableTimeRangesQuery create(LocalDate date, ObjectId serviceId, int serviceDuration) {
            BookingPartitionKey identifier = BookingPartitionKey.Factory.create(date, serviceId);
            GetBookingTimeRangesQuery query = new GetBookingTimeRangesQuery(identifier);
            return new GetAvailableTimeRangesQuery(query, serviceDuration);
        }
    }
}
