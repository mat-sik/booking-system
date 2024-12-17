package com.github.matsik.booking.controller.request;

import com.github.matsik.booking.config.validation.ValidStartEnd;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;

import java.time.LocalDate;

@ValidStartEnd
public record CreateBookingRequest(

        @NotNull
        LocalDate date,

        @NotNull
        ObjectId serviceId,

        @NotNull
        ObjectId userId,

        @Min(value = 0, message = "Start must be 0 or greater")
        int start,

        @Min(value = 0, message = "End must be 0 or greater")
        int end
) {
}
