package com.github.matsik.command.adapter.out.cassandra;

import com.github.matsik.command.application.port.out.CreateBookingPort;
import com.github.matsik.command.application.port.out.DeleteBookingPort;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class BookingPersistenceCachingAdapter implements CreateBookingPort, DeleteBookingPort {

    private final BookingPersistenceService bookingPersistenceService;

    private final BookingCache bookingsCache;

    @Override
    public long findOverlappingBookingCount(BookingPartitionKey bookingPartitionKey, TimeRange timeRange) {
        return bookingsCache.findOverlappingBookingCount(bookingPartitionKey, timeRange);
    }

    @Override
    public UUID createBooking(BookingPartitionKey bookingPartitionKey, UUID userId, TimeRange timeRange) {
        UUID bookingId = bookingPersistenceService.createBooking(bookingPartitionKey, userId, timeRange);
        bookingsCache.add(bookingPartitionKey, bookingId, userId, timeRange);
        return bookingId;
    }

    @Override
    public Optional<UUID> findBookingOwner(BookingPartitionKey bookingPartitionKey, UUID bookingId) {
        return bookingsCache.findBookingOwner(bookingPartitionKey, bookingId);
    }

    @Override
    public void deleteBooking(BookingPartitionKey bookingPartitionKey, UUID userId, UUID bookingId) {
        bookingPersistenceService.deleteBooking(bookingPartitionKey, userId, bookingId);
        bookingsCache.delete(bookingPartitionKey, bookingId);
    }

}
