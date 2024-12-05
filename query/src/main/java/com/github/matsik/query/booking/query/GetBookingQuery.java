package com.github.matsik.query.booking.query;

import org.bson.types.ObjectId;

public record GetBookingQuery(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId bookingId) {
}
