package com.github.matsik.command.application.domain;

import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.cassandra.entity.BookingByUser;
import com.github.matsik.dto.BookingPartitionKey;

import java.time.LocalDate;
import java.util.UUID;

class CassandraAdapterUtils {

    static Booking newBooking(UUID serviceId, UUID bookingId, LocalDate date, int start, int end) {
        BookingByServiceAndDate bookingByServiceAndDate = BookingByServiceAndDate.builder()
                .serviceId(serviceId)
                .date(date)
                .bookingId(bookingId)
                .userId(userId())
                .start(start)
                .end(end)
                .build();

        BookingByUser bookingByUser = BookingByUser.builder()
                .userId(userId())
                .serviceId(serviceId)
                .date(date)
                .bookingId(bookingId)
                .start(start)
                .end(end)
                .build();

        return new Booking(
                bookingByServiceAndDate,
                bookingByUser
        );
    }

    static Booking conflictingBooking(int start, int end) {
        BookingPartitionKey key = conflictingPartitionKey();
        return newBooking(
                key.serviceId(),
                UUID.randomUUID(),
                key.date(),
                start,
                end
        );
    }

    static Booking nonConflictingBooking(int start, int end) {
        BookingPartitionKey key = nonConflictingPartitionKey();
        return newBooking(
                key.serviceId(),
                UUID.randomUUID(),
                key.date(),
                start,
                end
        );
    }

    record Booking(BookingByServiceAndDate bookingByServiceAndDate, BookingByUser bookingByUser) {
    }

    static BookingPartitionKey nonConflictingOnServicePartitionKey() {
        return BookingPartitionKey.of(numberToUUID(2), numberToLocalDate(1));
    }

    static BookingPartitionKey nonConflictingOnDatePartitionKey() {
        return BookingPartitionKey.of(numberToUUID(1), numberToLocalDate(2));
    }

    static BookingPartitionKey nonConflictingPartitionKey() {
        return BookingPartitionKey.of(numberToUUID(3), numberToLocalDate(3));
    }

    static BookingPartitionKey conflictingPartitionKey() {
        return BookingPartitionKey.of(numberToUUID(1), numberToLocalDate(1));
    }

    static UUID nonExistingBookingId() {
        return numberToUUID(10);
    }

    static UUID nonExistingUserId() {
        return numberToUUID(2);
    }

    static UUID userId() {
        return numberToUUID(1);
    }

    static UUID numberToUUID(long number) {
        String uuidString = String.format("%08d-0000-0000-0000-000000000000", number);
        return UUID.fromString(uuidString);
    }

    static LocalDate numberToLocalDate(int number) {
        return LocalDate.of(2025, 9, number);
    }
}
