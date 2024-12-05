package com.github.matsik.query.request;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.bson.types.ObjectId;

public record GetBookingRequest(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId bookingId) {
    public static class Factory {
        public static GetBookingRequest create(String date, String serviceId, String bookingId) {
            ServiceBookingIdentifier identifier = ServiceBookingIdentifier.Factory.create(date, serviceId);
            ObjectId bookingIdMapped = new ObjectId(bookingId);
            return new GetBookingRequest(identifier, bookingIdMapped);
        }
    }
}
