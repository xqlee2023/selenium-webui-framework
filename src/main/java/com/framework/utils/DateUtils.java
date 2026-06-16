package com.framework.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtils() {}

    public static String timestamp() {
        return LocalDateTime.now().format(TIMESTAMP);
    }

    public static String date() {
        return LocalDateTime.now().format(DATE);
    }
}
