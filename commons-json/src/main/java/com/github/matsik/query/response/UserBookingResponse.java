package com.github.matsik.query.response;

import java.util.UUID;

public record UserBookingResponse(
        UUID userId,
        int start,
        int end
) {
}
