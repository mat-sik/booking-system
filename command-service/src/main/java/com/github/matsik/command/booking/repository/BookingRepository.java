package com.github.matsik.command.booking.repository;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.mongodb.client.result.UpdateResult;

public interface BookingRepository {
    UpdateResult deleteBooking(DeleteBookingCommand command);

    UpdateResult createBooking(CreateBookingCommand command);
}
