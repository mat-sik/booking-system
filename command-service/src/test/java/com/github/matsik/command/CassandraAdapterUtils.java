package com.github.matsik.command;

import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.cassandra.entity.BookingByUser;
import com.github.matsik.dto.BookingPartitionKey;

import java.time.LocalDate;
import java.util.UUID;

public class CassandraAdapterUtils {

    public static Booking newBooking(UUID serviceId, UUID bookingId, LocalDate date, int start, int end) {
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

    public static Booking conflictingBooking(int start, int end) {
        BookingPartitionKey key = conflictingPartitionKey();
        return newBooking(
                key.serviceId(),
                UUID.randomUUID(),
                key.date(),
                start,
                end
        );
    }

    public static Booking nonConflictingBooking(int start, int end) {
        BookingPartitionKey key = nonConflictingPartitionKey();
        return newBooking(
                key.serviceId(),
                UUID.randomUUID(),
                key.date(),
                start,
                end
        );
    }

    public record Booking(BookingByServiceAndDate bookingByServiceAndDate, BookingByUser bookingByUser) {
    }

    public static BookingPartitionKey nonConflictingOnServicePartitionKey() {
        return BookingPartitionKey.of(numberToUUID(2), numberToLocalDate(1));
    }

    public static BookingPartitionKey nonConflictingOnDatePartitionKey() {
        return BookingPartitionKey.of(numberToUUID(1), numberToLocalDate(2));
    }

    public static BookingPartitionKey nonConflictingPartitionKey() {
        return BookingPartitionKey.of(numberToUUID(3), numberToLocalDate(3));
    }

    public static BookingPartitionKey conflictingPartitionKey() {
        return BookingPartitionKey.of(numberToUUID(1), numberToLocalDate(1));
    }

    public static UUID nonExistingBookingId() {
        return numberToUUID(10);
    }

    public static UUID nonExistingUserId() {
        return numberToUUID(2);
    }

    public static UUID userId() {
        return numberToUUID(1);
    }

    public static UUID numberToUUID(long number) {
        String uuidString = String.format("%08d-0000-0000-0000-000000000000", number);
        return UUID.fromString(uuidString);
    }

    public static LocalDate numberToLocalDate(int number) {
        return LocalDate.of(2025, 9, number);
    }
}
