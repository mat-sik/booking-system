package com.github.matsik.command.booking.command;

import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.bson.types.ObjectId;

public record CreateBookingCommand(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId userId, int start,
                                   int end) {

    public static class Factory {
        public static CreateBookingCommand create(ServiceBookingIdentifier identifier, CreateBookingCommandValue value) {
            return new CreateBookingCommand(identifier, value.userId(), value.start(), value.end());
        }
    }
}
