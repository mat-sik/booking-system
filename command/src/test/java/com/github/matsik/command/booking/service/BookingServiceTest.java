package com.github.matsik.command.booking.service;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
class BookingServiceTest {

    private final BookingService service;

    @Autowired
    BookingServiceTest(BookingService service) {
        this.service = service;
    }

    @Test
    void deleteBooking() {
        LocalDate date = LocalDate.of(2024, 12, 3);
        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        ObjectId bookingId = new ObjectId("6752c16d66960f5b9bd2407e");

        ServiceBookingIdentifier serviceBookingIdentifier = ServiceBookingIdentifier.Factory.create(date, serviceId);
        DeleteBookingCommand deleteBookingCommand = new DeleteBookingCommand(serviceBookingIdentifier, bookingId);

        service.deleteBooking(deleteBookingCommand);
    }

    @Test
    void createBooking() {
        LocalDate date = LocalDate.of(2024, 12, 3);
        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        ObjectId userId = new ObjectId("bbbbbbbbbbbbbbbbbbbbbbba");
        int start = 0;
        int end = 30;

        ServiceBookingIdentifier serviceBookingIdentifier = ServiceBookingIdentifier.Factory.create(date, serviceId);
        CreateBookingCommand createBookingCommand = new CreateBookingCommand(serviceBookingIdentifier, userId, start, end);

        service.createBooking(createBookingCommand);
    }
}