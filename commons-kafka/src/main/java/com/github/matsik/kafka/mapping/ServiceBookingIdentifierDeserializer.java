package com.github.matsik.kafka.mapping;

import com.github.matsik.mongo.model.ServiceBookingIdentifier;
import org.apache.kafka.common.serialization.Deserializer;
import org.bson.types.ObjectId;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ServiceBookingIdentifierDeserializer implements Deserializer<ServiceBookingIdentifier> {

    private static final int OBJECT_ID_BYTE_AMOUNT = 12;

    @Override
    public ServiceBookingIdentifier deserialize(String topic, byte[] bytes) {
        return deserializeFromBytes(bytes);
    }

    private static ServiceBookingIdentifier deserializeFromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        ObjectId serviceId = new ObjectId(buffer);

        byte[] dateBytes = new byte[bytes.length - OBJECT_ID_BYTE_AMOUNT];
        buffer.get(dateBytes);

        String date = new String(dateBytes, StandardCharsets.UTF_8);

        return ServiceBookingIdentifier.Factory.create(date, serviceId);
    }

}
