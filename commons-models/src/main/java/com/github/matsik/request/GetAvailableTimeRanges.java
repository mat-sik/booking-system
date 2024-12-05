package com.github.matsik.request;

import com.github.matsik.request.query.GetBookingTimeRangesQuery;

public record GetAvailableTimeRanges(GetBookingTimeRangesQuery getBookingTimeRangesQuery, int serviceDuration) {
}
