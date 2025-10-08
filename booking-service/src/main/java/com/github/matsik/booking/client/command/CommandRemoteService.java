package com.github.matsik.booking.client.command;

import com.github.matsik.booking.controller.request.CreateBookingRequest;
import com.github.matsik.booking.controller.request.DeleteBookingRequest;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommandRemoteService {

    private final CommandClient client;

    public void createBooking(CreateBookingRequest request) {
        if (request.start() >= request.end()) {
            throw new IllegalArgumentException("Booking start value should be lower than the end value.");
        }

        BookingPartitionKey key = BookingPartitionKey.of(request.serviceId(), request.date());
        CreateBookingCommandValue value = new CreateBookingCommandValue(
                request.userId(),
                request.start(),
                request.end()
        );

        client.sendCreateBookingCommand(key, value);
    }

    public void deleteBooking(DeleteBookingRequest request) {
        BookingPartitionKey key = BookingPartitionKey.of(request.serviceId(), request.date());
        DeleteBookingCommandValue value = new DeleteBookingCommandValue(request.bookingId(), request.userId());

        client.sendDeleteBookingCommand(key, value);
    }

}
