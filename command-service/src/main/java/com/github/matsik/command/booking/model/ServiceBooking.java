package com.github.matsik.command.booking.model;

import com.github.matsik.mongo.model.Booking;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("service_bookings")
public record ServiceBooking(
        @Id ObjectId id,
        String date,
        ObjectId serviceId,
        List<Booking> bookings
) {
}
