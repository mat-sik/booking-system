package com.github.matsik.query.booking.query;

import com.github.matsik.dto.BookingPartitionKey;

import java.time.LocalDate;
import java.util.UUID;

public record GetAvailableTimeRangesQuery(BookingPartitionKey bookingPartitionKey, int serviceDuration) {
    public static class Factory {
        public static GetAvailableTimeRangesQuery create(UUID serviceId, LocalDate date, int serviceDuration) {
            BookingPartitionKey key = BookingPartitionKey.Factory.create(serviceId, date);
            return new GetAvailableTimeRangesQuery(key, serviceDuration);
        }
    }
}
