package com.github.matsik.booking.client.query;

import com.github.matsik.query.response.TimeRangeResponse;
import com.github.matsik.query.response.UserBookingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueryRemoteService {

    private static final String PREFIX = "/bookings";

    private final RestClient queryServiceClient;

    public ResponseEntity<List<TimeRangeResponse>> getAvailableTimeRanges(UUID serviceId, LocalDate date, int serviceDuration) {
        return queryServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PREFIX + "/available")
                        .queryParam("serviceId", serviceId.toString())
                        .queryParam("date", DateTimeFormatter.ISO_LOCAL_DATE.format(date))
                        .queryParam("serviceDuration", String.valueOf(serviceDuration))
                        .build())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }

    public ResponseEntity<TimeRangeResponse> getUserBookingTimeRange(
            UUID serviceId,
            LocalDate date,
            UUID userId,
            UUID bookingId
    ) {
        return queryServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PREFIX + "/user")
                        .queryParam("serviceId", serviceId.toString())
                        .queryParam("date", DateTimeFormatter.ISO_LOCAL_DATE.format(date))
                        .queryParam("userId", userId.toString())
                        .queryParam("bookingId", bookingId.toString())
                        .build())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }

    public ResponseEntity<List<UserBookingResponse>> getFirstUserBookings(UUID userId, int limit) {
        return queryServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PREFIX)
                        .queryParam("userId", userId.toString())
                        .queryParam("limit", String.valueOf(limit))
                        .build())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }

    public ResponseEntity<List<UserBookingResponse>> getNextUserBookings(
            UUID userId,
            UUID cursorServiceId,
            LocalDate cursorDate,
            UUID cursorBookingId,
            int limit
    ) {
        return queryServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PREFIX)
                        .queryParam("userId", userId.toString())
                        .queryParam("cursorServiceId", cursorServiceId.toString())
                        .queryParam("cursorDate", cursorDate.toString())
                        .queryParam("cursorBookingId", cursorBookingId.toString())
                        .queryParam("limit", String.valueOf(limit))
                        .build())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }

}
