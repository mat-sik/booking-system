package com.github.matsik.query.booking.query;

public record GetAvailableTimeRangesQuery(GetBookingTimeRangesQuery getBookingTimeRangesQuery, int serviceDuration) {
}
