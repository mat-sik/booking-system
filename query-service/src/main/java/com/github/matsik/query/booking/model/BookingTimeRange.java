package com.github.matsik.query.booking.model;

import org.bson.types.ObjectId;

public record BookingTimeRange(ObjectId id, int start, int end) {
}
