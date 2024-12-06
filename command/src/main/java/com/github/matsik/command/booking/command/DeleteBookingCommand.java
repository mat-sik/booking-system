package com.github.matsik.command.booking.command;

import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.bson.types.ObjectId;

import java.time.LocalDate;

public record DeleteBookingCommand(ServiceBookingIdentifier serviceBookingIdentifier, ObjectId bookingId) {
    public static class Factory {
        public static DeleteBookingCommand create(LocalDate key, DeleteBookingCommandValue value) {
            ServiceBookingIdentifier identifier = ServiceBookingIdentifier.Factory.create(key, value.serviceId());
            return new DeleteBookingCommand(identifier, value.bookingId());
        }
    }
}
