package com.github.matsik.query.booking.repository;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import com.github.matsik.query.booking.query.GetBookingQuery;
import com.github.matsik.query.booking.query.GetBookingTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingsQuery;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
class BookingRepositoryTest {

    private final BookingRepository repository;

    @Autowired
    BookingRepositoryTest(BookingRepository repository) {
        this.repository = repository;
    }

    @Test
    void getUserBooking() {
        LocalDate localDate = LocalDate.of(2024, 12, 3);
        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");

        ServiceBookingIdentifier identifier = ServiceBookingIdentifier.Factory.create(localDate, serviceId);

        ObjectId bookingId = new ObjectId("67500fcd910fab08c24c4ac1");
        GetBookingQuery request = new GetBookingQuery(identifier, bookingId);

        var out = repository.getUserBooking(request);
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
        GetBookingsQuery request = new GetBookingsQuery(dates, serviceIds, userIds);
        var out = repository.getBookings(request);
        System.out.println(out);
    }

    @Test
    void getBookingTimeRanges() {
        LocalDate localDate = LocalDate.of(2024, 12, 3);
        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        ServiceBookingIdentifier identifier = ServiceBookingIdentifier.Factory.create(localDate, serviceId);
        GetBookingTimeRangesQuery request = new GetBookingTimeRangesQuery(identifier);
        var out = repository.getBookingTimeRanges(request);
        System.out.println(out);
    }
}