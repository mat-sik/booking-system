package com.github.matsik.query.booking.repository;

import com.github.matsik.query.booking.query.GetBooking;
import com.github.matsik.query.booking.query.ServiceBookingIdentifier;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
    void getUserBooking() {
        LocalDate localDate = LocalDate.of(2024, 12, 3);
        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");

        ServiceBookingIdentifier identifier = new ServiceBookingIdentifier(localDate, serviceId);

        ObjectId bookingId = new ObjectId("67500fcd910fab08c24c4ac1");
        GetBooking request = new GetBooking(identifier, bookingId);

        var out = bookingRepository.getUserBooking(request);
        System.out.println(out);
    }

}