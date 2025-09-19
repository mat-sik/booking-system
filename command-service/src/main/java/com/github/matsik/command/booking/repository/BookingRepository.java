package com.github.matsik.command.booking.repository;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;

public interface BookingRepository {
    void deleteBooking(DeleteBookingCommand command);

    void createBooking(CreateBookingCommand command);
}
