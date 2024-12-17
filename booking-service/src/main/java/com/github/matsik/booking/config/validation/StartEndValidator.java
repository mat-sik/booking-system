package com.github.matsik.booking.config.validation;

import com.github.matsik.booking.controller.request.CreateBookingRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StartEndValidator implements ConstraintValidator<ValidStartEnd, CreateBookingRequest> {

    @Override
    public boolean isValid(CreateBookingRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // Let @NotNull handle null checks
        }
        return request.start() < request.end();
    }
}
