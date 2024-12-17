package com.github.matsik.booking.controller.request;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;

import java.time.LocalDate;

public record DeleteBookingRequest(

        @NotNull
        LocalDate date,

        @NotNull
        ObjectId serviceId,

        @NotNull
        ObjectId bookingId,

        @NotNull
        ObjectId userId
) {
}
