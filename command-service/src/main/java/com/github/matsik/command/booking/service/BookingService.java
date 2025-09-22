package com.github.matsik.command.booking.service;

import com.github.matsik.cassandra.model.Booking;
import com.github.matsik.cassandra.model.BookingPartitionKey;
import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    public void deleteBooking(DeleteBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        bookingRepository.deleteByPrimaryKey(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                command.bookingId()
        );
    }

    public void createBooking(CreateBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        long overlappingBookingCount = findOverlappingBookings(bookingPartitionKey, command.start(), command.end());
        if (overlappingBookingCount > 0) {
            return;
        }

        UUID bookingId = UUID.randomUUID();

        Booking booking = Booking.builder()
                .serviceId(bookingPartitionKey.serviceId())
                .date(bookingPartitionKey.date())
                .bookingId(bookingId)
                .start(command.start())
                .end(command.end())
                .userId(command.userId())
                .build();

        bookingRepository.save(booking);
    }

    private long findOverlappingBookings(BookingPartitionKey bookingPartitionKey, int start, int end) {
        return bookingRepository.findOverlappingBookingCount(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                start,
                end
        );
    }
}
