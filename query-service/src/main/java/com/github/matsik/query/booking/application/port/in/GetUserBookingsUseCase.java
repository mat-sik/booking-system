package com.github.matsik.query.booking.application.port.in;

import com.github.matsik.query.booking.application.domin.UserBooking;

import java.util.List;

public interface GetUserBookingsUseCase {

    List<UserBooking> getUserBookings(GetUserBookingsQuery query);
}
