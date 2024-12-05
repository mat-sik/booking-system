package com.github.matsik.request.query;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;

public record GetBookingTimeRangesQuery(ServiceBookingIdentifier serviceBookingIdentifier) {
}
