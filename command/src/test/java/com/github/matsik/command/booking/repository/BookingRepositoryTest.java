package com.github.matsik.command.booking.repository;

import com.github.matsik.command.booking.command.CreateBooking;
import com.github.matsik.command.booking.command.DeleteBooking;
import com.github.matsik.command.booking.command.ServiceBookingIdentifier;
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
        LocalDate date = LocalDate.of(2024, 12, 3);

        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        ObjectId userId = new ObjectId("bbbbbbbbbbbbbbbbbbbbbbbb");

        ServiceBookingIdentifier serviceBookingIdentifier = new ServiceBookingIdentifier(date, serviceId);
        CreateBooking createBooking = new CreateBooking(serviceBookingIdentifier, userId, 600, 600);
        UpdateResult out = bookingRepository.createBooking(createBooking);

        System.out.println(out);
    }

    @Test
    void deleteBooking_manual_test() {
        LocalDate date = LocalDate.of(2024, 12, 3);

        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        ObjectId bookingId = new ObjectId("674eff19d5c4604d2e4aa672");

        ServiceBookingIdentifier serviceBookingIdentifier = new ServiceBookingIdentifier(date, serviceId);
        DeleteBooking deleteBooking = new DeleteBooking(serviceBookingIdentifier, bookingId);
        UpdateResult out = bookingRepository.deleteBooking(deleteBooking);

        System.out.println(out);
    }
}