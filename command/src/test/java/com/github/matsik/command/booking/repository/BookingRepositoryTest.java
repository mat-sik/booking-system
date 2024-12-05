package com.github.matsik.command.booking.repository;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import com.mongodb.client.result.UpdateResult;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
class BookingRepositoryTest {

    private final BookingRepository bookingRepository;

    @Autowired
    BookingRepositoryTest(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Test
    void createBooking_manual_test() {
        LocalDate date = LocalDate.of(2024, 12, 4);

        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaab");
        ObjectId userId = new ObjectId("bbbbbbbbbbbbbbbbbbbbbbba");

        ServiceBookingIdentifier serviceBookingIdentifier = new ServiceBookingIdentifier(date, serviceId);
        CreateBookingCommand createBookingCommand = new CreateBookingCommand(serviceBookingIdentifier, userId, 720, 780);
        UpdateResult out = bookingRepository.createBooking(createBookingCommand);

        System.out.println(out);
    }

    @Test
    void deleteBooking_manual_test() {
        LocalDate date = LocalDate.of(2024, 12, 3);

        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        ObjectId bookingId = new ObjectId("67500fbf4752c73da3a096e5");

        ServiceBookingIdentifier serviceBookingIdentifier = new ServiceBookingIdentifier(date, serviceId);
        DeleteBookingCommand deleteBooking = new DeleteBookingCommand(serviceBookingIdentifier, bookingId);
        UpdateResult out = bookingRepository.deleteBooking(deleteBooking);

        System.out.println(out);
    }
}