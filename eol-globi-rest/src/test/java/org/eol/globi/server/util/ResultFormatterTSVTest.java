package org.eol.globi.server.util;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResultFormatterTSVTest {

    @Test
    public void toTSV() throws ResultFormattingException {
        String format = new ResultFormatterTSV().format("{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ -25.0, 135.0 ], [ 40.9777996, -79.5252906 ] ]}");
        assertThat(format, is("loc.latitude\tloc.longitude\n-25.0\t135.0\n40.9777996\t-79.5252906\n"));
    }

    @Test
    public void toTSVText() throws ResultFormattingException {
        String format = new ResultFormatterTSV().format("{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ \"one\", \"two\" ], [ \"three\", \"four\" ] ]}");
        assertThat(format, is("loc.latitude\tloc.longitude\none\ttwo\nthree\tfour\n"));
    }

    @Test
    public void toTSVQuotedText() throws ResultFormattingException {
        String format = new ResultFormatterTSV().format("{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ \"and he said: \\\"boo\\\"\", \"two\" ], [ \"three\", \"four\" ] ]}");
        assertThat(format, is("loc.latitude\tloc.longitude\nand he said: \"boo\"\ttwo\nthree\tfour\n"));
    }

}
