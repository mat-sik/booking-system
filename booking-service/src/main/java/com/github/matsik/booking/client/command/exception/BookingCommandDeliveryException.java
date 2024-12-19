package com.github.matsik.booking.client.command.exception;

public class BookingCommandDeliveryException extends RuntimeException {
    private static final String MESSAGE = "Failed to deliver the booking command to Kafka";

    public BookingCommandDeliveryException(Throwable cause) {
        super(MESSAGE, cause);
    }

}
