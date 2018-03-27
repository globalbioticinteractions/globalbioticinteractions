package org.eol.globi.util;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DateUtilTest {

    @Test
    public void formatDate() {
        assertThat(DateUtil.printDate(new Date(0)), is("1970-01-01T00:00:00Z"));
    }

    @Test
    public void parseDate() {
        String text = "2018-12";
        assertThat(DateUtil.parseYearMonthUTC(text).toString(), is("2018-12-01T00:00:00.000Z"));
        String text1 = "2018";
        assertThat(DateUtil.parseYearUTC(text1).toString(), is("2018-01-01T00:00:00.000Z"));
    }

}