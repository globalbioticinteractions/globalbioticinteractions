package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
        return DateTimeFormat
                .forPattern(pattern)
                .withZoneUTC()
                .parseDateTime(dateString);
    }

    public static DateTime parseDateUTC(String eventDate) {
        String firstInRange = splitPossibleRange(eventDate);

        DateTimeFormatter formatter = null;
        if (StringUtils.containsNone(firstInRange, '-', ':')) {
            if (StringUtils.contains(firstInRange, "T")) {
                formatter = ISODateTimeFormat.basicDateTimeNoMillis();
            } else {
                if (StringUtils.length(eventDate) == 8) {
                    formatter = ISODateTimeFormat.basicDate();
                }
            }
        }


        return (formatter == null ? ISODateTimeFormat.dateTimeParser() : formatter)
                .withZoneUTC()
                .parseDateTime(firstInRange);
    }

    public static DateTime parseDateUTC2(String eventDate) {

        String firstInRange = splitPossibleRange(eventDate);
        DateTimeFormatter formatter = null;
        if (StringUtils.contains(firstInRange, "T")) {
            formatter = StringUtils.containsAny(firstInRange, "-", ":")
                    ? ISODateTimeFormat.dateTimeParser()
                    : ISODateTimeFormat.basicDateTimeNoMillis();
        } else {
            formatter = StringUtils.containsAny(firstInRange, "-", ":")
                    ? ISODateTimeFormat.dateParser()
                    : ISODateTimeFormat.basicDate();
        }

        return formatter
                .withZoneUTC()
                .parseDateTime(firstInRange);
    }

    public static String splitPossibleRange(String eventDate) {
        return StringUtils.split(eventDate, "/")[0];
    }

    public static String nowDateString() {
        return ISODateTimeFormat.dateTime().withZoneUTC().print(new Date().getTime());
    }
}
