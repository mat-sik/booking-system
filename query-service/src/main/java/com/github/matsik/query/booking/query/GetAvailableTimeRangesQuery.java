package com.github.matsik.query.booking.query;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.bson.types.ObjectId;

import java.time.LocalDate;

public record GetAvailableTimeRangesQuery(GetBookingTimeRangesQuery getBookingTimeRangesQuery, int serviceDuration) {
    public static class Factory {
        public static GetAvailableTimeRangesQuery create(LocalDate date, ObjectId serviceId, int serviceDuration) {
            ServiceBookingIdentifier identifier = ServiceBookingIdentifier.Factory.create(date, serviceId);
            GetBookingTimeRangesQuery query = new GetBookingTimeRangesQuery(identifier);
            return new GetAvailableTimeRangesQuery(query, serviceDuration);
        }
    }
}
