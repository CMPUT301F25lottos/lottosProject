package com.example.lottos;

import com.google.firebase.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A utility class providing static helper methods for date and time conversions and formatting.
 *
 * Role: This class centralizes all logic related to parsing and formatting dates
 * throughout the application. It handles the conversion between different data types
 * used for time (Firebase {@link Timestamp}, {@link java.util.Date}, and {@link String})
 * and defines consistent formatting patterns for both user input and display.
 * The methods are thread-safe by synchronizing access to the SimpleDateFormat instances.
 */
public class TimeUtils {

    /**
     * The date-time format string used for displaying dates and times to the user (e.g., "2024/05/15 14:30").
     */
    public static final String EVENT_PATTERN_DISPLAY = "yyyy/MM/dd HH:mm";
    /**
     * The date-time format string expected from user input fields (e.g., "2024-05-15 14:30").
     */
    public static final String EVENT_PATTERN_INPUT   = "yyyy-MM-dd HH:mm";
    /**
     * A reusable, thread-safe formatter for displaying dates and times.
     */
    private static final SimpleDateFormat DISPLAY_FORMAT =
            new SimpleDateFormat(EVENT_PATTERN_DISPLAY, Locale.getDefault());

    /**
     * A reusable, thread-safe formatter for parsing date and time strings from input fields.
     */
    private static final SimpleDateFormat INPUT_FORMAT =
            new SimpleDateFormat(EVENT_PATTERN_INPUT, Locale.getDefault());

    /**
     * Formats a given time object into a human-readable string for display.
     * It first converts the object to a {@link Date} and then formats it using the {@code EVENT_PATTERN_DISPLAY}.
     *
     * @param value The time object to format, which can be a {@link Timestamp} or a {@link Date}.
     * @return A formatted string (e.g., "2024/05/15 14:30"), or "N/A" if the input is null or invalid.
     */
    public static String formatEventTime(Object value) {
        Date date = toDate(value);
        if (date == null) {
            return "N/A";
        }
        // Synchronize to ensure thread safety for the SimpleDateFormat instance.
        synchronized (DISPLAY_FORMAT) {
            return DISPLAY_FORMAT.format(date);
        }
    }

    /**
     * Parses a string from an input field into a {@link Date} object.
     * The string is expected to match the {@code EVENT_PATTERN_INPUT}.
     *
     * @param text The date-time string to parse.
     * @return A {@link Date} object representing the parsed time, or null if the input is null, empty, or malformed.
     */
    public static Date parseEventInput(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            // Synchronize to ensure thread safety for the SimpleDateFormat instance.
            synchronized (INPUT_FORMAT) {
                return INPUT_FORMAT.parse(text.trim());
            }
        } catch (ParseException e) {
            // Return null if the string does not match the expected format.
            return null;
        }
    }

    /**
     * Safely converts a generic object into a {@link Date} object.
     * This method can handle inputs of type {@link Timestamp} or {@link Date}.
     *
     * @param value The object to convert.
     * @return A {@link Date} object if the conversion is successful, otherwise null.
     */
    public static Date toDate(Object value) {
        if (value == null) return null;

        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate();
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        // Return null for any other type.
        return null;
    }
}
