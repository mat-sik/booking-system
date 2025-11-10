package com.github.matsik.query.booking.grpc;

import com.github.matsik.query.booking.service.exception.UserBookingNotFoundException;
import io.grpc.Status;
import io.grpc.StatusException;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.stereotype.Component;

@Component
public class QueryServiceGrpcExceptionHandler implements GrpcExceptionHandler {

    @Override
    public StatusException handleException(Throwable ex) {
        if (ex instanceof UserBookingNotFoundException) {
            return statusException(Status.NOT_FOUND, ex);
        } else if (ex instanceof IllegalArgumentException) {
            return statusException(Status.INVALID_ARGUMENT, ex);
        }
        return statusException(Status.INTERNAL, ex);
    }

    private StatusException statusException(Status status, Throwable ex) {
        return status
                .withDescription(ex.getMessage())
                .withCause(ex)
                .asException();
    }
}
