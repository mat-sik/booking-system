package com.github.matsik.query.booking.service;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import com.github.matsik.query.booking.model.BookingTimeRange;
import com.github.matsik.query.booking.query.GetAvailableTimeRangesQuery;
import com.github.matsik.query.booking.query.GetBookingTimeRangesQuery;
import com.github.matsik.query.booking.repository.BookingRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;

class BookingServiceTest {

    private final BookingRepository repository = Mockito.mock(BookingRepository.class);

    private final BookingService service = new BookingService(repository);

    @Test
    void getAvailableTimeRanges() {
        // given
        LocalDate localDate = LocalDate.of(2024, 12, 3);
        ObjectId serviceId = new ObjectId("aaaaaaaaaaaaaaaaaaaaaaaa");
        ServiceBookingIdentifier serviceBookingIdentifier = new ServiceBookingIdentifier(localDate, serviceId);
        GetBookingTimeRangesQuery getBookingTimeRangesQuery = new GetBookingTimeRangesQuery(serviceBookingIdentifier);
        GetAvailableTimeRangesQuery getAvailableTimeRangesQuery = new GetAvailableTimeRangesQuery(getBookingTimeRangesQuery, 55);

        List<BookingTimeRange> unavailableTimeRanges = List.of(
                new BookingTimeRange(new ObjectId(), 600, 660),
//                new BookingTimeRange(new ObjectId(), 660, 750),
                new BookingTimeRange(new ObjectId(), 810, 850)
        );

        given(repository.getBookingTimeRanges(getBookingTimeRangesQuery)).willReturn(unavailableTimeRanges);

        var out = service.getAvailableTimeRanges(getAvailableTimeRangesQuery);
        for (var i : out) {
            System.out.println(i);
        }
    }
}