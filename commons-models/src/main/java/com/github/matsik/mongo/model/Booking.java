package com.github.matsik.mongo.model;

import org.bson.types.ObjectId;

public record Booking(
        ObjectId id,
        ObjectId userId,
        int start,
        int end
) {
}
