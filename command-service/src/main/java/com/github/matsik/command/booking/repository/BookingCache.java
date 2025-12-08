package com.github.matsik.command.booking.repository;

import com.datastax.oss.driver.shaded.guava.common.base.Objects;
import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class BookingCache {

    private final BookingPersistenceAdapter bookingPersistenceAdapter;

    private final Map<BookingPartitionKey, Set<Booking>> bookingsCache;

    public void clear() {
        bookingsCache.clear();
    }

    long findOverlappingBookingCount(BookingPartitionKey bookingPartitionKey, TimeRange timeRange) {
        Set<Booking> bookings = fetch(bookingPartitionKey);
        return bookings.stream()
                .filter(booking -> isOverlap(booking, timeRange))
                .count();
    }

    private boolean isOverlap(Booking booking, TimeRange timeRange) {
        return booking.start < timeRange.start().minuteOfDay() && booking.end > timeRange.end().minuteOfDay();
    }

    Optional<UUID> findBookingOwner(BookingPartitionKey bookingPartitionKey, UUID bookingId) {
        Set<Booking> bookings = fetch(bookingPartitionKey);
        return bookings.stream()
                .filter(el -> findBy(el, bookingId))
                .findAny()
                .map(Booking::userId);
    }

    private boolean findBy(Booking booking, UUID bookingId) {
        return Objects.equal(booking.bookingId(), bookingId);
    }

    void add(BookingPartitionKey bookingPartitionKey, UUID bookingId, UUID userId, TimeRange timeRange) {
        Booking booking = Booking.of(bookingPartitionKey, bookingId, userId, timeRange);
        bookingsCache.computeIfAbsent(bookingPartitionKey, _ -> new HashSet<>())
                .add(booking);
    }

    void delete(BookingPartitionKey bookingPartitionKey, UUID bookingId, UUID userId) {
        Set<Booking> bookings = bookingsCache.get(bookingPartitionKey);
        if (bookings == null) {
            return;
        }
        bookings.stream()
                .filter(el -> findBy(el, bookingPartitionKey, bookingId, userId))
                .findAny()
                .ifPresent(bookings::remove);
    }

    private boolean findBy(Booking booking, BookingPartitionKey bookingPartitionKey, UUID bookingId, UUID userId) {
        return Objects.equal(booking.serviceId(), bookingPartitionKey.serviceId()) &&
                Objects.equal(booking.date(), bookingPartitionKey.date()) &&
                Objects.equal(booking.bookingId(), bookingId) &&
                Objects.equal(booking.userId(), userId);
    }

    private Set<Booking> fetch(BookingPartitionKey bookingPartitionKey) {
        Set<Booking> bookings = bookingsCache.get(bookingPartitionKey);
        if (bookings == null) {
            List<BookingByServiceAndDate> freshBookings = bookingPersistenceAdapter.findAllByServiceAndDate(
                    bookingPartitionKey.serviceId(),
                    bookingPartitionKey.date()
            );
            load(bookingPartitionKey, freshBookings);
        }
        return bookingsCache.get(bookingPartitionKey);
    }

    private void load(BookingPartitionKey key, List<BookingByServiceAndDate> freshBookings) {
        Set<Booking> bookings = bookingsCache.computeIfAbsent(key, _ -> new HashSet<>());
        for (BookingByServiceAndDate booking : freshBookings) {
            bookings.add(Booking.of(booking));
        }
    }

    private record Booking(
            UUID serviceId,
            LocalDate date,
            UUID bookingId,
            UUID userId,
            int start,
            int end
    ) {
        static Booking of(BookingPartitionKey bookingPartitionKey, UUID bookingId, UUID userId, TimeRange timeRange) {
            return new Booking(
                    bookingPartitionKey.serviceId(),
                    bookingPartitionKey.date(),
                    bookingId,
                    userId,
                    timeRange.start().minuteOfDay(),
                    timeRange.end().minuteOfDay()
            );
        }

        static Booking of(BookingByServiceAndDate booking) {
            return new Booking(
                    booking.serviceId(),
                    booking.date(),
                    booking.bookingId(),
                    booking.userId(),
                    booking.start(),
                    booking.end()
            );
        }
    }
}
