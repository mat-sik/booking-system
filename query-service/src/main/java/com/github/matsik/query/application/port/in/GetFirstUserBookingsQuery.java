package com.github.matsik.query.application.port.in;

import java.util.UUID;

public record GetFirstUserBookingsQuery(UUID userId, int limit) implements GetUserBookingsQuery {
}
