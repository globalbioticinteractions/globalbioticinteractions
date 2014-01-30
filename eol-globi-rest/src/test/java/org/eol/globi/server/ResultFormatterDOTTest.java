package org.eol.globi.server;

import org.hamcrest.core.Is;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class ResultFormatterDOTTest {

    @Test
    public void idTest() {
        assertThat(ResultFormatterDOT.getSafeLabel("EOL:123"), Is.is("EOL_123"));
        assertThat(ResultFormatterDOT.getSafeLabel("EOL//123"), Is.is("EOL__123"));
    }


}
