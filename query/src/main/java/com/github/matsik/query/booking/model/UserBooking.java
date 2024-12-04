package com.github.matsik.query.booking.model;

import org.bson.types.ObjectId;

public record UserBooking(
        ObjectId userId,
        int start,
        int end
) {
}
