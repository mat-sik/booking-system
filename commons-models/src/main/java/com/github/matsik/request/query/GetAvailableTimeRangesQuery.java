package com.github.matsik.request.query;

public record GetAvailableTimeRangesQuery(GetBookingTimeRangesQuery getBookingTimeRangesQuery, int serviceDuration) {
}
