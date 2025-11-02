package com.github.matsik.query.booking.query;

public sealed interface GetUserBookingsQuery permits GetFirstUserBookingsQuery, GetNextUserBookingsQuery {
}
