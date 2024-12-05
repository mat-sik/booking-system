package com.github.matsik.query.booking.query;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;

public record GetBookingTimeRangesQuery(ServiceBookingIdentifier serviceBookingIdentifier) {
}
