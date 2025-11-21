package com.easypan.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

public final class DateUtils {

    private DateUtils() {}

    // 默认时区
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    /**
     * 格式化 LocalDateTime 或 Date 为字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return dateTime.format(formatter);
    }

    public static String format(Date date, String pattern) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(date.toInstant(), DEFAULT_ZONE);
        return format(dateTime, pattern);
    }

    /**
     * 解析字符串为 LocalDateTime
     */
    public static LocalDateTime parseToLocalDateTime(String dateStr, String pattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDateTime.parse(dateStr, formatter);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return null; // 可以根据需求改为抛异常
        }
    }

    /**
     * 解析字符串为 Date
     */
    public static Date parse(String dateStr, String pattern) {
        LocalDateTime dateTime = parseToLocalDateTime(dateStr, pattern);
        return dateTime != null ? Date.from(dateTime.atZone(DEFAULT_ZONE).toInstant()) : null;
    }

    /**
     * 获取当前时间之后或之前的日期（天数可为负数）
     */
    public static LocalDateTime getAfterDateTime(int days) {
        return LocalDateTime.now().plusDays(days);
    }

    public static Date getAfterDate(int days) {
        return Date.from(getAfterDateTime(days).atZone(DEFAULT_ZONE).toInstant());
    }


}
