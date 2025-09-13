package com.github.matsik.booking.client.command;

import com.github.matsik.booking.client.query.QueryRemoteService;
import com.github.matsik.booking.controller.request.CreateBookingRequest;
import com.github.matsik.booking.controller.request.DeleteBookingRequest;
import com.github.matsik.kafka.task.CreateBookingCommandValue;
import com.github.matsik.kafka.task.DeleteBookingCommandValue;
import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import com.github.matsik.query.response.UserBookingResponse;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CommandRemoteService {

    private final CommandClient client;
    private final QueryRemoteService queryService;

    public void createBooking(CreateBookingRequest request) {
        if (request.start() >= request.end()) {
            throw new IllegalArgumentException("Booking start value should be lower than the end value.");
        }

        ServiceBookingIdentifier key = ServiceBookingIdentifier.Factory.create(request.date(), request.serviceId());

        CreateBookingCommandValue value = new CreateBookingCommandValue(
                request.userId(),
                request.start(),
                request.end()
        );

        client.sendCreateBookingCommand(key, value);
    }

    public void deleteBooking(DeleteBookingRequest request) {
        LocalDate localDate = request.date();
        ObjectId serviceId = request.serviceId();
        ObjectId bookingId = request.bookingId();
        ObjectId userId = request.userId();

        UserBookingResponse userBooking = queryService.getUserBooking(localDate, serviceId, bookingId);
        ObjectId bookingOwnerId = userBooking.userId();

        if (!Objects.equals(userId, bookingOwnerId)) {
            return;
        }

        ServiceBookingIdentifier key = ServiceBookingIdentifier.Factory.create(localDate, serviceId);

        DeleteBookingCommandValue value = new DeleteBookingCommandValue(bookingId);

        client.sendDeleteBookingCommand(key, value);
    }

}
