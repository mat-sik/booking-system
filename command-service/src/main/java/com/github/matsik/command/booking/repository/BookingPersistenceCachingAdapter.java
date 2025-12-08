package com.github.matsik.command.booking.repository;

import com.datastax.oss.driver.shaded.guava.common.base.Objects;
import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.command.booking.service.BookingCommandsPort;
import com.github.matsik.dto.BookingPartitionKey;

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

    private final Map<BookingPartitionKey, Set<Booking>> bookingsCache;

    public BookingPersistenceCachingAdapter(BookingPersistenceAdapter bookingPersistenceAdapter) {
        this.bookingPersistenceAdapter = bookingPersistenceAdapter;

        this.bookingsCache = new HashMap<>();
    }

    private void clear() {
        bookingsCache.clear();
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

    @Override
    public long findOverlappingBookingCount(UUID serviceId, LocalDate date, int start, int end) {
        Set<Booking> bookings = fetch(BookingPartitionKey.of(serviceId, date));
        return bookings.stream()
                .filter(el -> el.start() < end && el.end() > start)
                .count();
    }

    @Override
    public UUID createBooking(UUID serviceId, LocalDate date, UUID userId, int start, int end) {
        UUID bookingId = bookingPersistenceAdapter.createBooking(serviceId, date, userId, start, end);
        Booking booking = new Booking(serviceId, date, bookingId, userId, start, end);
        add(booking);
        return bookingId;
    }

    private void add(Booking booking) {
        BookingPartitionKey key = BookingPartitionKey.of(booking.serviceId(), booking.date());
        bookingsCache.computeIfAbsent(key, _ -> new HashSet<>())
                .add(booking);
    }

    @Override
    public Optional<UUID> findBookingOwner(UUID serviceId, LocalDate date, UUID bookingId) {
        Set<Booking> bookings = fetch(BookingPartitionKey.of(serviceId, date));
        return bookings.stream()
                .filter(el -> findBy(el, bookingId))
                .findAny()
                .map(Booking::userId);
    }

    private boolean findBy(Booking booking, UUID bookingId) {
        return Objects.equal(booking.bookingId(), bookingId);
    }

    @Override
    public void deleteBooking(UUID serviceId, LocalDate date, UUID userId, UUID bookingId) {
        bookingPersistenceAdapter.deleteBooking(serviceId, date, userId, bookingId);
        delete(serviceId, date, bookingId, userId);
    }

    private void delete(UUID serviceId, LocalDate date, UUID bookingId, UUID userId) {
        Set<Booking> bookings = bookingsCache.get(BookingPartitionKey.of(serviceId, date));
        if (bookings == null) {
            return;
        }
        bookings.stream()
                .filter(el -> findBy(el, serviceId, date, bookingId, userId))
                .findAny()
                .ifPresent(bookings::remove);
    }

    private boolean findBy(Booking booking, UUID serviceId, LocalDate date, UUID bookingId, UUID userId) {
        return Objects.equal(booking.serviceId(), serviceId) &&
                Objects.equal(booking.date(), date) &&
                Objects.equal(booking.bookingId(), bookingId) &&
                Objects.equal(booking.userId(), userId);
    }

    private record Booking(
            UUID serviceId,
            LocalDate date,
            UUID bookingId,
            UUID userId,
            int start,
            int end
    ) {
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
