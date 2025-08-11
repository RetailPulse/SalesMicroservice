package com.retailpulse.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    public static String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // Function to convert a String to Instant with a custom date-time format
    public static Instant convertStringToInstant(String dateTime, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);
        return localDateTime.toInstant(ZoneOffset.ofHours(8));
    }

    // Function to convert Instant to formatted String
    public static String convertInstantToString(Instant instant, String pattern) {
        // Convert Instant to ZonedDateTime using the specified time zone
        ZoneId zoneId = ZoneId.of("Asia/Singapore"); // Example: "UTC", "America/New_York", etc.
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);

        // Create a DateTimeFormatter with the specified pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        // Format the ZonedDateTime to String
        return zonedDateTime.format(formatter);
    }

}
