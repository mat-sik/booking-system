package com.github.matsik.booking;

import com.github.matsik.booking.client.command.exception.BookingCommandDeliveryException;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<byte[]> onFeignException(FeignException ex) {
        HttpStatusCode statusCode = HttpStatusCode.valueOf(ex.status());

        Optional<ByteBuffer> body = ex.responseBody();

        return body.map(bodyValue -> {
            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.put("content-type", List.of("application/problem+json"));

            byte[] bodyBytes = bodyValue.array();

            return new ResponseEntity<>(bodyBytes, headers, statusCode);
        }).orElseGet(() -> {
            String message = ex.getMessage();
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

            return new ResponseEntity<>(messageBytes, statusCode);
        });
    }

    @ExceptionHandler(BookingCommandDeliveryException.class)
    public ProblemDetail onBookingCommandDeliveryException(BookingCommandDeliveryException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail onHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail onIllegalArgumentException(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail onIllegalStateException(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onMethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException ex) {
        String errorMessage = extractErrorMessages(ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errorMessage);
    }

    private static String extractErrorMessages(MethodArgumentNotValidException ex) {
        List<String> errorMessages = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .sorted() // to get deterministic order of error messages
                .toList();
        return String.join(", ", errorMessages);
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
                .sorted() // to get deterministic order of error messages
                .collect(Collectors.joining(", "));
    }

}
