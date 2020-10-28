package org.eol.globi.util;

import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
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

}