package com.github.matsik.query.booking.query;

import org.bson.types.ObjectId;

public record GetBooking(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId bookingId) {
}
