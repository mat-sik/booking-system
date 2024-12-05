package com.github.matsik.command.booking.command;

import org.bson.types.ObjectId;

public record DeleteBookingCommand(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId bookingId) {
}
