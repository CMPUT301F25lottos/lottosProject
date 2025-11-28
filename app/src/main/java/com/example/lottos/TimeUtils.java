package com.example.lottos;

import com.google.firebase.Timestamp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    public static final String EVENT_PATTERN_DISPLAY = "yyyy/MM/dd HH:mm";
    public static final String EVENT_PATTERN_INPUT   = "yyyy-MM-dd HH:mm";
    private static final SimpleDateFormat DISPLAY_FORMAT =
            new SimpleDateFormat(EVENT_PATTERN_DISPLAY, Locale.getDefault());

    private static final SimpleDateFormat INPUT_FORMAT =
            new SimpleDateFormat(EVENT_PATTERN_INPUT, Locale.getDefault());

    public static String formatEventTime(Object value) {
        Date date = toDate(value);
        if (date == null) {
            return "N/A";
        }
        synchronized (DISPLAY_FORMAT) {
            return DISPLAY_FORMAT.format(date);
        }
    }

    public static Date parseEventInput(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            synchronized (INPUT_FORMAT) {
                return INPUT_FORMAT.parse(text.trim());
            }
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date toDate(Object value) {
        if (value == null) return null;

        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate();
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        return null;
    }
}
