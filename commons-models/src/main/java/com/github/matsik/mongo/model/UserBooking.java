package com.github.matsik.mongo.model;

import org.bson.types.ObjectId;

public record UserBooking(
        ObjectId userId,
        int start,
        int end
) {
}
