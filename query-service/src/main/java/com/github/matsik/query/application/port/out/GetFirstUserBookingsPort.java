package com.github.matsik.query.application.port.out;

import com.github.matsik.query.application.domin.UserBooking;

import java.util.List;
import java.util.UUID;

public interface GetFirstUserBookingsPort {

    List<UserBooking> getFirstUserBookings(UUID userId, int limit);
}
