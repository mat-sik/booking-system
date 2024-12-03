package com.github.matsik.command.booking.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

public record Booking(
        @Id ObjectId id,
        ObjectId userId,
        int start,
        int end
) {
}
