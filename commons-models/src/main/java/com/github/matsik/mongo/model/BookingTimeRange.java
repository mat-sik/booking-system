package com.github.matsik.mongo.model;

import org.bson.types.ObjectId;

public record BookingTimeRange(ObjectId id, int start, int end) {
}
