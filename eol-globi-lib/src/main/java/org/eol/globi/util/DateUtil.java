package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Date;
import java.util.Map;

public final class DateUtil {
    private static DateTimeFormatter basicYearDateFormatter;

    public static String printDate(Date date) {
        return date == null ? "" : ISODateTimeFormat.dateTimeNoMillis().withZoneUTC().print(date.getTime());
    }

    public static String printDateTime(DateTime dateTime) {
        return printDate(dateTime.toDate());
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
                } else if (StringUtils.length(eventDate) == 6) {
                    if (basicYearDateFormatter == null) {
                        basicYearDateFormatter = new DateTimeFormatterBuilder()
                                .appendYear(4, 4)
                                .appendFixedDecimal(DateTimeFieldType.monthOfYear(), 2)
                                .toFormatter();
                    }
                    formatter = basicYearDateFormatter;
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

    public static boolean hasStartDateAfterEndDate(String eventDate) {
        boolean hasStartedAfterFinishing = false;
        final String[] split = StringUtils.split(eventDate, "/");
        if (split.length > 1) {
            try {
                Interval actual = Interval.parse(eventDate);
                hasStartedAfterFinishing = actual.getStart().isAfter(actual.getEnd());
            } catch (IllegalArgumentException ex) {
                final int diff = StringUtils.length(split[0]) - StringUtils.length(split[1]);
                if (diff > 0) {
                    final String prefix = StringUtils.substring(split[0], 0, diff);
                    final String attemptWorkaround = StringUtils.join(split[0], "/", prefix + split[1]);
                    try {
                        Interval actual = Interval.parse(attemptWorkaround);
                        hasStartedAfterFinishing = actual.getStart().isAfter(actual.getEnd());
                    } catch (IllegalArgumentException e) {
                        throw e;
                    }
                } else {
                    throw ex;
                }
            }
        }
        return hasStartedAfterFinishing;
    }

    public static String validateDate(String eventDate, DateTime dateTime) {
        String msg = null;
        if (dateTime.getYear() == 8888) {
            // 8888 is a magic number used by Arctos
            // see http://handbook.arctosdb.org/documentation/dates.html#restricted-data
            // https://github.com/ArctosDB/arctos/issues/2426
            msg = "date [" + printDateTime(dateTime) + "] appears to be restricted, see http://handbook.arctosdb.org/documentation/dates.html#restricted-data";
        } else if (dateTime.isAfter(new DateTime())) {
            msg = "date [" + printDateTime(dateTime) + "] is in the future";
        } else if (dateTime.getYear() < 100) {
            msg = "date [" + printDateTime(dateTime) + "] occurred in the first century AD";
        } else {
            try {
                hasStartDateAfterEndDate(eventDate);
            } catch (IllegalArgumentException ex) {
                msg = "issue handling date range [" + eventDate + "]: " + ex.getMessage();
            }
        }
        return msg;
    }
}
