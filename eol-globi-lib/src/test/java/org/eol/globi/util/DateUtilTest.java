package org.eol.globi.util;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;

public class DateUtilTest {

    @Test
    public void formatDate() {
        assertThat(DateUtil.printDate(new Date(0)), is("1970-01-01T00:00:00Z"));
    }

    @Test
    public void parseDateYearMonth() {
        String text = "2018-12";
        assertThat(DateUtil.parseYearMonthUTC(text).toString(), is("2018-12-01T00:00:00.000Z"));
    }

    @Test
    public void parseDateYearMonthRange() {
        String text = "2018-12/2019-01";
        assertThat(DateUtil.parseYearMonthUTC(text).toString(), is("2018-12-01T00:00:00.000Z"));
    }

    @Test
    public void parseDateYearMonthRange2() {
        String text = "2006-07/08";
        assertThat(DateUtil.parseYearMonthUTC(text).toString(), is("2006-07-01T00:00:00.000Z"));
    }

    @Test
    public void parseYear() {
        String text1 = "2018";
        assertThat(DateUtil.parseYearUTC(text1).toString(), is("2018-01-01T00:00:00.000Z"));
    }

    @Test
    public void parseYearRange() {
        String text1 = "2018/2019";
        assertThat(DateUtil.parseYearUTC(text1).toString(), is("2018-01-01T00:00:00.000Z"));
    }

    @Test
    public void parseDateUTCRange() {
        DateTime dateTime = DateUtil.parseDateUTC("2002-07-02/2003-01-07");
        assertNotNull(dateTime);
        assertThat(dateTime.getDayOfMonth(), is(2));
    }

    @Test
    public void parseDateUTC() {
        DateTime dateTime = DateUtil.parseDateUTC("2002-07-02");
        assertNotNull(dateTime);
        assertThat(dateTime.getDayOfMonth(), is(2));
        assertThat(dateTime.getYear(), is(2002));
        assertThat(dateTime.getMonthOfYear(), is(7));
    }

    @Test
    public void parseDateISO8601() {
        DateTime dateTime = DateUtil.parseDateUTC("20020702");
        assertNotNull(dateTime);
        assertThat(dateTime.getYear(), is(2002));
        assertThat(dateTime.getDayOfMonth(), is(2));
        assertThat(dateTime.getMonthOfYear(), is(7));
    }

    @Test
    public void parseDateISO8601DateTime() {
        DateTime dateTime = DateUtil.parseDateUTC("20201119T221644Z");
        assertNotNull(dateTime);
        assertThat(dateTime.getYear(), is(2020));
        assertThat(dateTime.getDayOfMonth(), is(19));
        assertThat(dateTime.getMonthOfYear(), is(11));
        assertThat(dateTime.getHourOfDay(), is(22));
        assertThat(dateTime.getMinuteOfHour(), is(16));
        assertThat(dateTime.getSecondOfMinute(), is(44));
    }

    @Test
    public void parseDateISO8601Year() {
        DateTime dateTime = DateUtil.parseDateUTC("2016");
        assertNotNull(dateTime);
        assertThat(dateTime.getYear(), is(2016));
    }

    @Test
    public void parseDateISO8601YearMonth() {
        DateTime dateTime = DateUtil.parseDateUTC("201604");
        assertNotNull(dateTime);
        assertThat(dateTime.getYear(), is(2016));
        assertThat(dateTime.getMonthOfYear(), is(04));
    }


    @Test(expected = IllegalArgumentException.class)
    public void startDateAfterEndDateInvalidEndDate() {
        assertFalse(DateUtil.hasStartDateAfterEndDate("1973-07-01/1973-09-31"));
    }

    @Test
    public void startDateAfterEndDate() {
        assertFalse(DateUtil.hasStartDateAfterEndDate("1973-07-01/1973-09-30"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void startDateAfterEndDate2() {
        assertTrue(DateUtil.hasStartDateAfterEndDate("1973-09-30/1973-07-01"));
    }


    @Test
    public void startDateEndDateYearMonth() {
        assertFalse(DateUtil.hasStartDateAfterEndDate("2006-07/08"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void startedMonthAfterEnding() {
        Assert.assertTrue(DateUtil.hasStartDateAfterEndDate("2006-09/08"));
    }

    @Test
    public void startDateEndDateYearMonth2() {
        assertFalse(DateUtil.hasStartDateAfterEndDate("2006-07/2006-08"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void startAfterEnding() {
        Assert.assertTrue(DateUtil.hasStartDateAfterEndDate("2008-07/2006-08"));
    }

    @Test
    public void validateDate() {
        String dateRange = "1973-07-01/1973-09-31";
        String s = DateUtil.validateDate(
                dateRange, DateUtil.parseDateUTC(dateRange)
        );

        assertThat(s, is("issue handling date range [1973-07-01/1973-09-31]: Cannot parse \"1973-09-31\": Value 31 for dayOfMonth must be in the range [1,30]"));

    }


}