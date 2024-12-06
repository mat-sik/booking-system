package com.github.matsik.kafka.task;

import org.bson.types.ObjectId;

public record DeleteBookingCommandValue(ObjectId serviceId, ObjectId bookingId) implements CommandValue {
}
