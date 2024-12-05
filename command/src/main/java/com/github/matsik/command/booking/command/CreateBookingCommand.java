package com.github.matsik.command.booking.command;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.bson.types.ObjectId;

public record CreateBookingCommand(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId userId, int start,
                                   int end) {
}
