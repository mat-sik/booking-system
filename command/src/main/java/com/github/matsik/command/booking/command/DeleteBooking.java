package com.github.matsik.command.booking.command;

import org.bson.types.ObjectId;

public record DeleteBooking(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId bookingId) {
}
