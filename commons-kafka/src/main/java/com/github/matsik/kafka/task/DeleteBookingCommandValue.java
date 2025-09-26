package com.github.matsik.kafka.task;

import java.util.UUID;

public record DeleteBookingCommandValue(UUID bookingId, UUID userId) implements CommandValue {
}
