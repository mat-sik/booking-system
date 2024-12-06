package com.github.matsik.kafka.task;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeleteBookingCommandValue.class),
        @JsonSubTypes.Type(value = CreateBookingCommandValue.class)
})
public interface CommandValue {
}
