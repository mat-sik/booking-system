package com.github.matsik.kafka.mapping;

import com.github.matsik.cassandra.model.BookingPartitionKey;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.UUID;

public class BookingPartitionKeySerializer implements Serializer<BookingPartitionKey> {

    public static final int PARTITION_KEY_SIZE = 20;

    @Override
    public byte[] serialize(String topic, BookingPartitionKey identifier) {
        return serializeToBytes(identifier);
    }

    private static byte[] serializeToBytes(BookingPartitionKey identifier) {
        UUID serviceId = identifier.serviceId();
        long serviceIdMostSignificantBits = serviceId.getMostSignificantBits();
        long serviceIdLeastSignificantBits = serviceId.getLeastSignificantBits();

        LocalDate date =  identifier.date();
        int dateBytes = (int) date.toEpochDay(); // the range should be sufficient for bookings

        ByteBuffer buffer = ByteBuffer.allocate(PARTITION_KEY_SIZE);
        buffer.putLong(serviceIdMostSignificantBits);
        buffer.putLong(serviceIdLeastSignificantBits);
        buffer.putInt(dateBytes);

        return buffer.array();
    }

}
