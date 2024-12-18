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
        if (isProfileActive(LOGGING_PROFILE_NAME)) {
            logDeleteBooking(command, result);
        }
    }

    private void logDeleteBooking(DeleteBookingCommand command, UpdateResult result) {
        String message = getDeleteBookingLogMessage(command, result);
        log.info(String.format("%s Result: Matched count: %d, Deleted count: %d",
                message, result.getMatchedCount(), result.getModifiedCount()));
    }

    private static String getDeleteBookingLogMessage(DeleteBookingCommand command, UpdateResult result) {
        String date = command.serviceBookingIdentifier().date();
        String serviceId = command.serviceBookingIdentifier().serviceId().toString();
        String bookingId = command.bookingId().toString();

        if (result.getMatchedCount() == 1) {
            if (result.getModifiedCount() != 1) {
                return String.format(
                        "Delete failed: day: %s, service: %s, booking ID: %s. Reason: No booking with the specified ID found.",
                        date, serviceId, bookingId);
            }
            return String.format(
                    "Successfully deleted booking: day: %s, service: %s, booking ID: %s",
                    date, serviceId, bookingId);
        }
        return String.format(
                "Delete failed: day: %s, service: %s, booking ID: %s. Reason: No matching booking found.",
                date, serviceId, bookingId);
    }

    public void createBooking(CreateBookingCommand command) {
        UpdateResult result = repository.createBooking(command);
        if (isProfileActive(LOGGING_PROFILE_NAME)) {
            logCreateBooking(command, result);
        }
    }

    private void logCreateBooking(CreateBookingCommand command, UpdateResult result) {
        String message = getCreateBookingLogMessage(command, result);
        log.info(String.format("%s Result: Matched count: %d, Modified count: %d",
                message, result.getMatchedCount(), result.getModifiedCount()));
    }

    private static String getCreateBookingLogMessage(CreateBookingCommand command, UpdateResult result) {
        String date = command.serviceBookingIdentifier().date();
        String serviceId = command.serviceBookingIdentifier().serviceId().toString();
        int start = command.start();
        int end = command.end();
        String userId = command.userId().toString();

        if (result.getMatchedCount() == 1) {
            return String.format(
                    "Successfully booked: day: %s, service: %s, time-range: (%d - %d), user: %s",
                    date, serviceId, start, end, userId);
        }
        return String.format(
                "Booking failed: day: %s, service: %s, time-range: (%d - %d), user: %s. Reason: Time range is already taken.",
                date, serviceId, start, end, userId);
    }

    private boolean isProfileActive(String profile) {
        return Arrays.asList(environment.getActiveProfiles()).contains(profile);
    }
}
