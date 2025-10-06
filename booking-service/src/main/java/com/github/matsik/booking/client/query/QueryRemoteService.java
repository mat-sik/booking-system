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

    private static final String PREFIX = "/booking";

    private final RestClient queryServiceClient;

    public ResponseEntity<List<TimeRangeResponse>> getAvailableTimeRanges(LocalDate date, UUID serviceId, int serviceDuration) {
        return queryServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PREFIX + "/available")
                        .queryParam("date", DateTimeFormatter.ISO_LOCAL_DATE.format(date))
                        .queryParam("serviceId", serviceId.toString())
                        .queryParam("serviceDuration", String.valueOf(serviceDuration))
                        .build())
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }

    public ResponseEntity<TimeRangeResponse> getUserBookingTimeRange(
            LocalDate date,
            UUID serviceId,
            UUID userId,
            UUID bookingId
    ) {
        return queryServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PREFIX)
                        .queryParam("date", DateTimeFormatter.ISO_LOCAL_DATE.format(date))
                        .queryParam("serviceId", serviceId.toString())
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
                        .path(PREFIX + "/many")
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
                        .path(PREFIX + "/many")
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
