package com.example.marketplace.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.of("UTC"));

    public static String formatUtc(Instant instant) {
        if (instant == null)
            return null;
        return ISO_FORMATTER.format(instant);
    }

    public static Instant parseUtc(String isoString) {
        if (isoString == null || isoString.isBlank())
            return null;
        return Instant.from(ISO_FORMATTER.parse(isoString));
    }
}
