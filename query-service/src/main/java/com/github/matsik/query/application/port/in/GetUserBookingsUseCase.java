package com.github.matsik.query.application.port.in;

import com.github.matsik.query.application.domin.UserBooking;

import java.util.List;

public interface GetUserBookingsUseCase {

    List<UserBooking> getUserBookings(GetUserBookingsQuery query);
}
