package com.github.matsik.request.command;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.bson.types.ObjectId;

public record DeleteBookingCommand(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId bookingId) {
}
