package com.github.matsik.query.request;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import jakarta.validation.constraints.Positive;

public record GetAvailableTimeRangesRequest(
        ServiceBookingIdentifier serviceBookingIdentifier,
        @Positive int serviceDuration
) {
    public static class Factory {
        public static GetAvailableTimeRangesRequest create(String date, String serviceId, int serviceDuration) {
            ServiceBookingIdentifier identifier = ServiceBookingIdentifier.Factory.create(date, serviceId);
            return new GetAvailableTimeRangesRequest(identifier, serviceDuration);
        }
    }
}
