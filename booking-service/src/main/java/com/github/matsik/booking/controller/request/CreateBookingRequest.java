package com.github.matsik.booking.controller.request;

import org.bson.types.ObjectId;

import java.time.LocalDate;

public record CreateBookingRequest(LocalDate date, ObjectId serviceId, ObjectId userId, int start, int end) {
}
