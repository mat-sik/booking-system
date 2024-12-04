package com.github.matsik.query.booking.repository;

import com.github.matsik.query.booking.query.GetBooking;
import com.github.matsik.query.booking.query.GetBookingTimeRanges;
import com.github.matsik.query.booking.query.GetBookings;
import com.github.matsik.query.booking.query.ServiceBookingIdentifier;
import net.bytebuddy.asm.Advice;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

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

    @Test
    void getBookings() {
        List<LocalDate> dates = List.of(
                LocalDate.of(2024, 12, 3),
                LocalDate.of(2024, 12, 4)
        );
        List<ObjectId> serviceIds = List.of(new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa"));
        List<ObjectId> userIds = List.of(new ObjectId("bbbbbbbbbbbbbbbbbbbbbbbb"));
        GetBookings request = new GetBookings(dates, serviceIds, userIds);
        var out = bookingRepository.getBookings(request);
        System.out.println(out);
    }

    @Test
    void getBookingTimeRanges() {
        LocalDate localDate = LocalDate.of(2024, 12, 3);
        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        ServiceBookingIdentifier identifier = new ServiceBookingIdentifier(localDate, serviceId);
        GetBookingTimeRanges request = new GetBookingTimeRanges(identifier);
        var out = bookingRepository.getBookingTimeRanges(request);
        System.out.println(out);
    }
}