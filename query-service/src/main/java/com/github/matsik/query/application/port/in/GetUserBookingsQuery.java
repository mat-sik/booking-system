package com.github.matsik.query.application.port.in;

public sealed interface GetUserBookingsQuery permits GetFirstUserBookingsQuery, GetNextUserBookingsQuery {
}
