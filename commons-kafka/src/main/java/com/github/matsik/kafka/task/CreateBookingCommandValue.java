package com.github.matsik.kafka.task;

import java.util.UUID;

public record CreateBookingCommandValue(UUID userId, int start, int end) implements CommandValue {
}
