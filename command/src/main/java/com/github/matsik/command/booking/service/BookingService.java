package com.github.matsik.command.booking.service;

import com.github.matsik.command.booking.command.CreateBookingCommand;
import com.github.matsik.command.booking.command.DeleteBookingCommand;
import com.github.matsik.command.booking.repository.BookingRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Log
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final String LOGGING_PROFILE_NAME = "logging-profile";

    private final BookingRepository repository;
    private final Environment environment;

    public void deleteBooking(DeleteBookingCommand command) {
        UpdateResult result = repository.deleteBooking(command);
        logDeleteBooking(command, result);
    }

    private void logDeleteBooking(DeleteBookingCommand command, UpdateResult result) {
        if (isProfileActive(LOGGING_PROFILE_NAME)) {
            String message;
            String date = command.serviceBookingIdentifier().date();
            String serviceId = command.serviceBookingIdentifier().serviceId().toString();
            String bookingId = command.bookingId().toString();

            if (result.getMatchedCount() == 1) {
                if (result.getModifiedCount() != 1) {
                    message = String.format(
                            "Delete failed: day: %s, service: %s, booking ID: %s. Reason: No booking with the specified ID found.",
                            date, serviceId, bookingId);
                } else {
                    message = String.format(
                            "Successfully deleted booking: day: %s, service: %s, booking ID: %s",
                            date, serviceId, bookingId);
                }
            } else {
                message = String.format(
                        "Delete failed: day: %s, service: %s, booking ID: %s. Reason: No matching booking found.",
                        date, serviceId, bookingId);
            }

            // Log the message along with the result details
            log.info(String.format("%s Result: Matched count: %d, Deleted count: %d",
                    message, result.getMatchedCount(), result.getModifiedCount()));
        }
    }

    public void createBooking(CreateBookingCommand command) {
        UpdateResult result = repository.createBooking(command);
        logCreateBooking(command, result);
    }

    private void logCreateBooking(CreateBookingCommand command, UpdateResult result) {
        if (isProfileActive(LOGGING_PROFILE_NAME)) {
            String message;
            String date = command.serviceBookingIdentifier().date();
            String serviceId = command.serviceBookingIdentifier().serviceId().toString();
            int start = command.start();
            int end = command.end();
            String userId = command.userId().toString();

            if (result.getMatchedCount() == 1) {
                if (result.getModifiedCount() != 1) {
                    message = String.format(
                            "Booking failed despite matching: day: %s, service: %s, time-range: (%d - %d), user: %s. Result: Modified count is not 1",
                            date, serviceId, start, end, userId);
                    log.severe(message);
                    return;
                }
                message = String.format(
                        "Successfully booked: day: %s, service: %s, time-range: (%d - %d), user: %s",
                        date, serviceId, start, end, userId);
            } else {
                message = String.format(
                        "Booking failed: day: %s, service: %s, time-range: (%d - %d), user: %s. Reason: Time range is already taken.",
                        date, serviceId, start, end, userId);
            }

            // Log the message along with the result details
            log.info(String.format("%s Result: Matched count: %d, Modified count: %d",
                    message, result.getMatchedCount(), result.getModifiedCount()));
        }
    }

    private boolean isProfileActive(String profile) {
        return Arrays.asList(environment.getActiveProfiles()).contains(profile);
    }
}
