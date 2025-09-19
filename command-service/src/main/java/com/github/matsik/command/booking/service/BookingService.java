package com.github.matsik.command.booking.service;

import com.github.matsik.cassandra.model.Booking;
import com.github.matsik.cassandra.model.BookingKey;
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

        BookingKey key = new BookingKey(bookingPartitionKey.serviceId(), bookingPartitionKey.date(), command.bookingId());
        bookingRepository.deleteById(key);
    }

    public void createBooking(CreateBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        int overlappingBookingCount = findOverlappingBookings(bookingPartitionKey, command.start(), command.end());
        if (overlappingBookingCount > 0) {
            return;
        }

        UUID bookingId = UUID.randomUUID();
        BookingKey key = new BookingKey(bookingPartitionKey.serviceId(), bookingPartitionKey.date(), bookingId);

        Booking booking = new Booking(key, command.userId(), command.start(), command.end());
        bookingRepository.save(booking);
    }

    private int findOverlappingBookings(BookingPartitionKey bookingPartitionKey, int start, int end) {
        return bookingRepository.findOverlappingBookingCount(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                start,
                end
        );
    }
}
