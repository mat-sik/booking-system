package com.github.matsik.kafka.mapping;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ServiceBookingIdentifierSerializer implements Serializer<ServiceBookingIdentifier> {

    @Override
    public byte[] serialize(String topic, ServiceBookingIdentifier identifier) {
        return serializeToBytes(identifier);
    }

    private static byte[] serializeToBytes(ServiceBookingIdentifier identifier) {
        byte[] serviceIdBytes = identifier.serviceId().toByteArray();
        byte[] dateBytes = identifier.date().getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(serviceIdBytes.length + dateBytes.length);
        buffer.put(serviceIdBytes);
        buffer.put(dateBytes);

        return buffer.array();
    }

}
