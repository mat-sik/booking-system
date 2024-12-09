package com.github.matsik.booking.controller.request;

import org.bson.types.ObjectId;

import java.time.LocalDate;

public record DeleteBookingRequest(LocalDate date, ObjectId serviceId, ObjectId bookingId, ObjectId userId) {
}
