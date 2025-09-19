package com.github.matsik.query.booking.query;

import com.github.matsik.cassandra.model.BookingPartitionKey;

public record GetBookingTimeRangesQuery(BookingPartitionKey bookingPartitionKey) {
}
