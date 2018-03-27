package org.eol.globi.util;

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
        return ISODateTimeFormat.year().withZoneUTC().parseDateTime(dateString);
    }

    public static DateTime parseYearMonthUTC(String dateString) {
        return ISODateTimeFormat.yearMonth().withZoneUTC().parseDateTime(dateString);
    }

    public static DateTime parsePatternUTC(String dateString, String pattern) {
        return DateTimeFormat.forPattern(pattern).withZoneUTC().parseDateTime(dateString);
    }
}
