package com.github.matsik.command.booking.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.github.matsik.cassandra.model.BookingByServiceAndDate;
import com.github.matsik.cassandra.model.BookingByUser;
import com.github.matsik.cassandra.model.BookingPartitionKey;
import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final CqlSession session;
    private final BookingRepository bookingRepository;

    public void deleteBooking(DeleteBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        Optional<UUID> ownerId = bookingRepository.findBookingOwner(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                command.bookingId()
        );

        if (ownerId.isEmpty() || !Objects.equals(ownerId.get(), command.userId())) {
            return;
        }

        BoundStatement deleteBookingByServiceAndDate = bookingRepository.deleteByPrimaryKey(
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                command.bookingId()
        );

        BoundStatement deleteBookingByUser = bookingRepository.deleteByPrimaryKey(
                command.userId(),
                bookingPartitionKey.serviceId(),
                bookingPartitionKey.date(),
                command.bookingId()
        );

        BatchStatement batchStatement = BatchStatement.builder(DefaultBatchType.LOGGED)
                .addStatement(deleteBookingByServiceAndDate)
                .addStatement(deleteBookingByUser)
                .build();

        session.execute(batchStatement);
    }

    public Optional<UUID> createBooking(CreateBookingCommand command) {
        BookingPartitionKey bookingPartitionKey = command.bookingPartitionKey();

        long overlappingBookingCount = findOverlappingBookings(bookingPartitionKey, command.start(), command.end());
        if (overlappingBookingCount > 0) {
            return Optional.empty();
        }

        UUID bookingId = UUID.randomUUID();

        BookingByServiceAndDate bookingByServiceAndDate = BookingByServiceAndDate.builder()
                .serviceId(bookingPartitionKey.serviceId())
                .date(bookingPartitionKey.date())
                .bookingId(bookingId)
                .start(command.start())
                .end(command.end())
                .userId(command.userId())
                .build();

        BoundStatement createBookingByServiceAndDate = bookingRepository.save(bookingByServiceAndDate);

        BookingByUser bookingByUser = BookingByUser.builder()
                .userId(command.userId())
                .serviceId(bookingPartitionKey.serviceId())
                .date(bookingPartitionKey.date())
                .bookingId(bookingId)
                .start(command.start())
                .end(command.end())
                .build();

        BoundStatement createBookingByUser = bookingRepository.save(bookingByUser);

        BatchStatement batchStatement = BatchStatement.builder(DefaultBatchType.LOGGED)
                .addStatement(createBookingByServiceAndDate)
                .addStatement(createBookingByUser)
                .build();

        session.execute(batchStatement);

        return Optional.of(bookingId);
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
