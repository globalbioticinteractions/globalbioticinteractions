package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Date;

public final class DateUtil {
    public static String printDate(Date date) {
        return date == null ? "" : ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(date.getTime());
    }

    public static String printYear(Date date) {
        return date == null ? "" : ISODateTimeFormat.year().withZoneUTC().print(date.getTime());
    }

    public static DateTime parseYearUTC(String dateString) {
        String firstInRange = splitPossibleRange(dateString);
        return ISODateTimeFormat.year().withZoneUTC().parseDateTime(firstInRange);
    }

    public static DateTime parseYearMonthUTC(String dateString) {
        String firstInRange = splitPossibleRange(dateString);
        return ISODateTimeFormat.yearMonth().withZoneUTC().parseDateTime(firstInRange);
    }

    public static DateTime parsePatternUTC(String dateString, String pattern) {
        String firstInRange = splitPossibleRange(dateString);
        return DateTimeFormat.forPattern(pattern).withZoneUTC().parseDateTime(firstInRange);
    }

    public static DateTime parseDateUTC(String eventDate) {
        String firstInRange = splitPossibleRange(eventDate);
        return ISODateTimeFormat.dateTimeParser().withZoneUTC().parseDateTime(firstInRange);
    }

    public static String splitPossibleRange(String eventDate) {
        return StringUtils.split(eventDate, "/")[0];
    }
}
