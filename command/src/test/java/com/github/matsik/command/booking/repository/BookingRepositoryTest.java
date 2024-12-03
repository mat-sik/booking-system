package com.github.matsik.command.booking.repository;

import com.github.matsik.command.booking.command.CreateBooking;
import com.github.matsik.command.booking.command.CreateServiceBooking;
import com.mongodb.client.result.UpdateResult;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.format.datetime.DateFormatter;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

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
        ObjectId userId = new ObjectId();

        CreateServiceBooking createServiceBooking = new CreateServiceBooking(date, serviceId);
        CreateBooking createBooking = new CreateBooking(createServiceBooking, userId, 600, 600);
        UpdateResult out = bookingRepository.createBooking(createBooking);
        System.out.println(out);
    }

}