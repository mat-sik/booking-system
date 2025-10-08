package com.github.matsik.query.booking.query;

import com.github.matsik.dto.BookingPartitionKey;

import java.time.LocalDate;
import java.util.UUID;

public record GetAvailableTimeRangesQuery(BookingPartitionKey bookingPartitionKey, int serviceDuration) {
    public static GetAvailableTimeRangesQuery of(UUID serviceId, LocalDate date, int serviceDuration) {
        BookingPartitionKey key = BookingPartitionKey.of(serviceId, date);
        return new GetAvailableTimeRangesQuery(key, serviceDuration);
    }
}
