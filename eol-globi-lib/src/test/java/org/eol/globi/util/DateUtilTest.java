package org.eol.globi.util;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class DateUtilTest {

    @Test
    public void formatDate() {
        assertThat(DateUtil.printDate(new Date(0)), Is.is("1970-01-01T00:00:00Z"));
    }

}