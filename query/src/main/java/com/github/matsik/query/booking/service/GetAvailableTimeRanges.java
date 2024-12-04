package com.github.matsik.query.booking.service;

import com.github.matsik.query.booking.query.GetBookingTimeRanges;

public record GetAvailableTimeRanges(GetBookingTimeRanges getBookingTimeRanges, int serviceDuration) {
}
