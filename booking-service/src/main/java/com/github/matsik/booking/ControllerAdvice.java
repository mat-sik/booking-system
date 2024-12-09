package com.github.matsik.booking;

import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.FieldError;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail onMethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException ex) {
        String errorMessage = extractErrorMessages(ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errorMessage);
    }

    private static String extractErrorMessages(MethodArgumentNotValidException ex) {
        List<String> errorMessages = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
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
                .collect(Collectors.joining(", "));
    }

}
