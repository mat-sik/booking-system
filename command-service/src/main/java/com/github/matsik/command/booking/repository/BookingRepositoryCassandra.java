package com.github.matsik.command.booking.repository;

import com.github.matsik.cassandra.model.Booking;
import com.github.matsik.cassandra.model.BookingKey;
import com.github.matsik.cassandra.model.BookingPartitionKey;
import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

public interface BookingRepositoryCassandra extends BookingRepository, CassandraRepository<Booking, BookingKey> {

    @Override
    default void deleteBooking(DeleteBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();
        BookingKey key = new BookingKey(bookingPartitionKey.serviceId(), bookingPartitionKey.date(), command.bookingId());
        this.deleteById(key);
    }

    @Override
    default void createBooking(CreateBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        UUID bookingId = UUID.randomUUID();
        BookingKey key = new BookingKey(bookingPartitionKey.serviceId(), bookingPartitionKey.date(), bookingId);

        Booking booking = new Booking(key, command.userId(), command.start(), command.end());
        this.save(booking);
    }
}
