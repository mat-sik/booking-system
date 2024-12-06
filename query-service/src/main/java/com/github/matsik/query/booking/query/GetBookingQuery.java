package com.github.matsik.query.booking.query;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.bson.types.ObjectId;

import java.time.LocalDate;

public record GetBookingQuery(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId bookingId) {
    public static class Factory {
        public static GetBookingQuery create(LocalDate date, ObjectId serviceId, ObjectId bookingId) {
            ServiceBookingIdentifier identifier = ServiceBookingIdentifier.Factory.create(date, serviceId);
            return new GetBookingQuery(identifier, bookingId);
        }
    }
}
