package com.github.matsik.booking.controller.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record DeleteBookingRequest(

        @NotNull(message = "Date cannot be null")
        LocalDate date,

        @NotNull(message = "Service Id cannot be null")
        UUID serviceId,

        @NotNull(message = "Booking Id cannot be null")
        UUID bookingId,

        @NotNull(message = "User Id cannot be null")
        UUID userId
) {
}
