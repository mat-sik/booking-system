package com.github.matsik.booking.client.command;

import com.github.matsik.booking.controller.request.CreateBookingRequest;
import com.github.matsik.booking.controller.request.DeleteBookingRequest;
import com.github.matsik.cassandra.model.BookingPartitionKey;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommandRemoteService {

    private final CommandClient client;

    public void createBooking(CreateBookingRequest request) {
        if (request.start() >= request.end()) {
            throw new IllegalArgumentException("Booking start value should be lower than the end value.");
        }

        BookingPartitionKey key = BookingPartitionKey.Factory.create(request.date(), request.serviceId());

        CreateBookingCommandValue value = new CreateBookingCommandValue(
                request.userId(),
                request.start(),
                request.end()
        );

        client.sendCreateBookingCommand(key, value);
    }

    public void deleteBooking(DeleteBookingRequest request) {
        LocalDate localDate = request.date();
        UUID serviceId = request.serviceId();
        UUID bookingId = request.bookingId();
        UUID userId = request.userId();

        // TODO: verify is user is the owner

        BookingPartitionKey key = BookingPartitionKey.Factory.create(localDate, serviceId);

        DeleteBookingCommandValue value = new DeleteBookingCommandValue(bookingId, userId);

        client.sendDeleteBookingCommand(key, value);
    }

}
