package com.github.matsik.query.booking.application.port.in;

import java.util.UUID;

public record GetFirstUserBookingsQuery(UUID userId, int limit) implements GetUserBookingsQuery {
}
