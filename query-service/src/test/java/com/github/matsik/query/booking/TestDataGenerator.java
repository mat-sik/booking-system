package com.github.matsik.query.booking;

import java.time.LocalDate;
import java.util.UUID;

public class TestDataGenerator {
    public static UUID numberToUUID(long number) {
        String uuidString = String.format("%08d-0000-0000-0000-000000000000", number);
        return UUID.fromString(uuidString);
    }

    public static LocalDate numberToLocalDate(int number) {
        return LocalDate.of(2025, 9, number);
    }
}