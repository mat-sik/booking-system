package com.github.matsik.command.booking.repository;

import com.datastax.oss.driver.shaded.guava.common.base.Objects;
import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.command.booking.service.BookingCommandsPort;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BookingPersistenceCachingAdapter implements BookingCommandsPort {

    private final BookingPersistenceAdapter bookingPersistenceAdapter;

    private final BookingCache bookingsCache;

    public BookingPersistenceCachingAdapter(BookingPersistenceAdapter bookingPersistenceAdapter) {
        this.bookingPersistenceAdapter = bookingPersistenceAdapter;

        this.bookingsCache = new BookingCache(new HashMap<>());
    }

    @Override
    public long findOverlappingBookingCount(BookingPartitionKey bookingPartitionKey, TimeRange timeRange) {
        Set<Booking> bookings = bookingsCache.fetch(bookingPartitionKey);
        return bookings.stream()
                .filter(booking -> isOverlap(booking, timeRange))
                .count();
    }

    private boolean isOverlap(Booking booking, TimeRange timeRange) {
        return booking.start < timeRange.start().minuteOfDay() && booking.end > timeRange.end().minuteOfDay();
    }

    @Override
    public UUID createBooking(BookingPartitionKey bookingPartitionKey, UUID userId, TimeRange timeRange) {
        UUID bookingId = bookingPersistenceAdapter.createBooking(bookingPartitionKey, userId, timeRange);
        Booking booking = Booking.of(bookingPartitionKey, bookingId, userId, timeRange);
        bookingsCache.add(booking);
        return bookingId;
    }

    @Override
    public Optional<UUID> findBookingOwner(BookingPartitionKey bookingPartitionKey, UUID bookingId) {
        Set<Booking> bookings = bookingsCache.fetch(bookingPartitionKey);
        return bookings.stream()
                .filter(el -> findBy(el, bookingId))
                .findAny()
                .map(Booking::userId);
    }

    private boolean findBy(Booking booking, UUID bookingId) {
        return Objects.equal(booking.bookingId(), bookingId);
    }

    @Override
    public void deleteBooking(BookingPartitionKey bookingPartitionKey, UUID userId, UUID bookingId) {
        bookingPersistenceAdapter.deleteBooking(bookingPartitionKey, userId, bookingId);
        bookingsCache.delete(bookingPartitionKey, bookingId, userId);
    }

    private boolean findBy(Booking booking, BookingPartitionKey bookingPartitionKey, UUID bookingId, UUID userId) {
        return Objects.equal(booking.serviceId(), bookingPartitionKey.serviceId()) &&
                Objects.equal(booking.date(), bookingPartitionKey.date()) &&
                Objects.equal(booking.bookingId(), bookingId) &&
                Objects.equal(booking.userId(), userId);
    }

    @RequiredArgsConstructor
    private class BookingCache {

        private final Map<BookingPartitionKey, Set<Booking>> bookingsCache;

        private void clear() {
            bookingsCache.clear();
        }

        private void add(Booking booking) {
            BookingPartitionKey key = BookingPartitionKey.of(booking.serviceId(), booking.date());
            bookingsCache.computeIfAbsent(key, _ -> new HashSet<>())
                    .add(booking);
        }

        private void delete(BookingPartitionKey bookingPartitionKey, UUID bookingId, UUID userId) {
            Set<Booking> bookings = bookingsCache.get(bookingPartitionKey);
            if (bookings == null) {
                return;
            }
            bookings.stream()
                    .filter(el -> findBy(el, bookingPartitionKey, bookingId, userId))
                    .findAny()
                    .ifPresent(bookings::remove);
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
