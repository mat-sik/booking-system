package com.github.matsik.command.booking.command;

import org.bson.types.ObjectId;

public record CreateBookingCommand(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId userId, int start, int end) {
}
