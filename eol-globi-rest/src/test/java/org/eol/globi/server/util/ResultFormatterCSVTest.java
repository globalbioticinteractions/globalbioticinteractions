package org.eol.globi.server.util;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResultFormatterCSVTest {

    @Test
    public void toCSV() throws ResultFormattingException {
        String format = new ResultFormatterCSV().format("{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ -25.0, 135.0 ], [ 40.9777996, -79.5252906 ] ]}");
        assertThat(format, is("\"loc.latitude\",\"loc.longitude\"\n-25.0,135.0\n40.9777996,-79.5252906\n"));
    }

    @Test
    public void toCSVText() throws ResultFormattingException {
        String format = new ResultFormatterCSV().format("{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ \"one\", \"two\" ], [ \"three\", \"four\" ] ]}");
        assertThat(format, is("\"loc.latitude\",\"loc.longitude\"\n\"one\",\"two\"\n\"three\",\"four\"\n"));
    }

    @Test
    public void toCSVQuotedText() throws ResultFormattingException {
        String format = new ResultFormatterCSV().format("{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ \"and he said: \\\"boo\\\"\", \"two\" ], [ \"three\", \"four\" ] ]}");
        assertThat(format, is("\"loc.latitude\",\"loc.longitude\"\n\"and he said: \"\"boo\"\"\",\"two\"\n\"three\",\"four\"\n"));
    }

}
