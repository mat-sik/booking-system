package com.github.matsik.query.response;

import com.github.matsik.mongo.model.Booking;
import org.bson.types.ObjectId;

import java.util.List;

public record ServiceBookingResponse(
        ObjectId id,
        String date,
        ObjectId serviceId,
        List<Booking> bookings
) {
}
