package com.kollectivemobile.euki.cycleig;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CycleIgDates {
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private CycleIgDates() {
    }

    public static String formatDate(Date date) {
        return formatter().format(date);
    }

    public static Date parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return startOfDay(formatter().parse(value.trim()));
        } catch (ParseException exception) {
            throw new IllegalArgumentException("Use YYYY-MM-DD dates for the SMART Link range.");
        }
    }

    public static Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startOfDay(date));
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    private static SimpleDateFormat formatter() {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN, Locale.US);
        formatter.setLenient(false);
        return formatter;
    }
}
