package org.eol.globi.util;

import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Date;

public final class DateUtil {
    public static String printDate(Date date) {
        return date == null ? "" : ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC).print(date.getTime());
    }
}
