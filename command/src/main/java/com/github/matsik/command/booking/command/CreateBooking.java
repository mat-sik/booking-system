package com.github.matsik.command.booking.command;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bson.types.ObjectId;

@Getter
@Accessors(fluent = true)
public class CreateBooking {

    private final CreateServiceBooking createServiceBooking;
    private final ObjectId userId;
    private final int start;
    private final int end;

    public CreateBooking(CreateServiceBooking createServiceBooking, ObjectId userId, int start, int end) {
        this.createServiceBooking = createServiceBooking;
        this.userId = userId;
        this.start = start;
        this.end = end;
    }

}
