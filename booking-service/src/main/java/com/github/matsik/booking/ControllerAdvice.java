package com.github.matsik.booking;

import com.github.matsik.booking.client.command.exception.BookingCommandDeliveryException;
import com.github.matsik.booking.client.query.exception.UserBookingNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(UserBookingNotFoundException.class)
    public ProblemDetail onUserBookingNotFoundException(UserBookingNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BookingCommandDeliveryException.class)
    public ProblemDetail onBookingCommandDeliveryException(BookingCommandDeliveryException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, problemDetailMessage(ex));
    }

    private String problemDetailMessage(MethodArgumentTypeMismatchException ex) {
        String propertyName = ex.getName();
        Object wrongValue = ex.getValue();
        return String.format("Parameter: '%s' has incorrect value: '%s'", propertyName, wrongValue);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail onConstraintViolationException(ConstraintViolationException ex) {
        String errorMessage = extractErrorMessages(ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errorMessage);
    }

    private static String extractErrorMessages(ConstraintViolationException ex) {
        return ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", "));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail onMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

}
