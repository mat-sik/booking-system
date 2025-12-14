package com.github.matsik.query.booking.application.port.in;

public sealed interface GetUserBookingsQuery permits GetFirstUserBookingsQuery, GetNextUserBookingsQuery {
}
