package com.github.matsik.query;

import com.github.matsik.cassandra.entity.BookingByServiceAndDate;
import com.github.matsik.cassandra.entity.BookingByUser;
import com.github.matsik.dto.BookingPartitionKey;
import com.github.matsik.dto.TimeRange;
import com.github.matsik.query.application.domin.UserBooking;

import java.time.LocalDate;
import java.util.UUID;

public class CassandraAdapterUtils {

    public static UserBooking userBooking(UUID bookingId, int start, int end) {
        BookingPartitionKey key = aBookingPartitionKey();
        return new UserBooking(
                key.serviceId(),
                key.date(),
                bookingId,
                TimeRange.of(start, end)
        );
    }

    public static Booking booking(int start, int end) {
        UUID bookingId = UUID.randomUUID();
        UUID userId = aUserId();
        return booking(bookingId, userId, start, end);
    }

    public static Booking booking(UUID bookingId, UUID userId, int start, int end) {
        BookingPartitionKey key = aBookingPartitionKey();
        return newBooking(key.serviceId(), key.date(), bookingId, userId, start, end);
    }

    public static BookingPartitionKey aBookingPartitionKey() {
        return BookingPartitionKey.of(TestDataGenerator.numberToUUID(1), TestDataGenerator.numberToLocalDate(1));
    }

    private static Booking newBooking(UUID serviceId, LocalDate date, UUID bookingId, UUID userId, int start, int end) {
        BookingByServiceAndDate bookingByServiceAndDate = BookingByServiceAndDate.builder()
                .serviceId(serviceId)
                .date(date)
                .bookingId(bookingId)
                .userId(userId)
                .start(start)
                .end(end)
                .build();

        BookingByUser bookingByUser = BookingByUser.builder()
                .userId(userId)
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

    public record Booking(BookingByServiceAndDate bookingByServiceAndDate, BookingByUser bookingByUser) {
    }

    public static UUID aUserId() {
        return TestDataGenerator.numberToUUID(1);
    }

    public static UUID bUserId() {
        return TestDataGenerator.numberToUUID(2);
    }
}
