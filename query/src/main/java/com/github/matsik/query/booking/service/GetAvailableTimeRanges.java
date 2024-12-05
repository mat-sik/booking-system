package com.github.matsik.query.booking.service;

import com.github.matsik.query.booking.query.GetBookingTimeRangesQuery;

public record GetAvailableTimeRanges(GetBookingTimeRangesQuery getBookingTimeRangesQuery, int serviceDuration) {
}
