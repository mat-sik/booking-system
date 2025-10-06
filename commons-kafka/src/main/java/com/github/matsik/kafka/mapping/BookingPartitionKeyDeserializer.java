package com.github.matsik.kafka.mapping;

import com.github.matsik.cassandra.model.BookingPartitionKey;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.UUID;

import static com.github.matsik.kafka.mapping.BookingPartitionKeySerializer.PARTITION_KEY_SIZE;

public class BookingPartitionKeyDeserializer implements Deserializer<BookingPartitionKey> {

    @Override
    public BookingPartitionKey deserialize(String topic, byte[] bytes) {
        return deserializeFromBytes(bytes);
    }

    private static BookingPartitionKey deserializeFromBytes(byte[] bytes) {
        if (bytes.length != PARTITION_KEY_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Invalid byte array length: %d, expected %d", bytes.length, PARTITION_KEY_SIZE)
            );
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        long mostSignificantBits = buffer.getLong();
        long leastSignificantBits = buffer.getLong();
        UUID serviceId = new UUID(mostSignificantBits, leastSignificantBits);

        int epochDay = buffer.getInt();
        LocalDate date = LocalDate.ofEpochDay(epochDay);

        return BookingPartitionKey.Factory.create(serviceId, date);
    }

}
