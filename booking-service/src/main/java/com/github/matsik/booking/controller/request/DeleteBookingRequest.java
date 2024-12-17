package com.github.matsik.booking.controller.request;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;

import java.time.LocalDate;

public record DeleteBookingRequest(

        @NotNull(message = "Date cannot be null")
        LocalDate date,

        @NotNull(message = "Service Id cannot be null")
        ObjectId serviceId,

        @NotNull(message = "Booking Id cannot be null")
        ObjectId bookingId,

        @NotNull(message = "User Id cannot be null")
        ObjectId userId
) {
}
