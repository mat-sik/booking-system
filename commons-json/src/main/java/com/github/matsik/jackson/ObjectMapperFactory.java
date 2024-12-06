package com.github.matsik.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bson.types.ObjectId;

public class ObjectMapperFactory {

    public static ObjectMapper create() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(ObjectId.class, new ObjectIdSerializer());
        module.addDeserializer(ObjectId.class, new ObjectIdDeserializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);

        return mapper;
    }

}
