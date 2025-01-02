package com.github.matsik.kafka.task;

import org.bson.types.ObjectId;

public record CreateBookingCommandValue(ObjectId userId, int start, int end) implements CommandValue {
}
