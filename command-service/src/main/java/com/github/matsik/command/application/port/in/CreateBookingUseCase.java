package com.github.matsik.command.application.port.in;

import java.util.Optional;
import java.util.UUID;

public interface CreateBookingUseCase {

    Optional<UUID> createBooking(CreateBookingCommand command);
}
