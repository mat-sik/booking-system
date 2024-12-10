package com.github.matsik.query.response;

import org.bson.types.ObjectId;

public record UserBookingResponse(
        ObjectId userId,
        int start,
        int end
) {
}
