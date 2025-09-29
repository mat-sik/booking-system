package com.github.matsik.query.booking.query;

import com.github.matsik.cassandra.model.BookingPartitionKey;

import java.time.LocalDate;
import java.util.UUID;

public record GetAvailableTimeRangesQuery(BookingPartitionKey bookingPartitionKey, int serviceDuration) {
    public static class Factory {
        public static GetAvailableTimeRangesQuery create(LocalDate date, UUID serviceId, int serviceDuration) {
            BookingPartitionKey key = BookingPartitionKey.Factory.create(date, serviceId);
            return new GetAvailableTimeRangesQuery(key, serviceDuration);
        }
    }
}
