package com.github.matsik.command.booking.command;

import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.bson.types.ObjectId;

public record DeleteBookingCommand(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId bookingId) {
    public static class Factory {
        public static DeleteBookingCommand create(ServiceBookingIdentifier identifier, DeleteBookingCommandValue value) {
            return new DeleteBookingCommand(identifier, value.bookingId());
        }
    }
}
