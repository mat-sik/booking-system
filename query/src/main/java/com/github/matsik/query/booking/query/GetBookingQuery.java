package com.github.matsik.query.booking.query;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.bson.types.ObjectId;

public record GetBookingQuery(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId bookingId) {
}
