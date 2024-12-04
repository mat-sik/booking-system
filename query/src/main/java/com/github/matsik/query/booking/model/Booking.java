package com.github.matsik.query.booking.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

public record Booking(
        ObjectId id,
        ObjectId userId,
        int start,
        int end
) {
}
