package com.github.matsik.query.booking.query;

import java.util.UUID;

public record GetFirstUserBookingsQuery(UUID userId, int limit) implements GetUserBookingsQuery {
}
