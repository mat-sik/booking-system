package com.github.matsik.command.booking.service;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

@Log
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository repository;

    public void deleteBooking(DeleteBookingCommand command) {
        repository.deleteBooking(command);
    }

    public void createBooking(CreateBookingCommand command) {
        repository.createBooking(command);
    }
}
