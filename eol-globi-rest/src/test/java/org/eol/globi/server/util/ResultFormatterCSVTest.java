package org.eol.globi.server.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResultFormatterCSVTest {

    public static final String CSV_WITH_TEXT_VALUES = "{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ \"one\", \"two\" ], [ \"three\", \"four\" ] ]}";
    public static final String CSV_WITH_QUOTED_TEXT_VALUES = "{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ \"and he said: \\\"boo\\\"\", \"two\" ], [ \"three\", \"four\" ] ]}";
    public static final String CSV_NUMERIC_VALUES = "{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ -25.0, 135.0 ], [ 40.9777996, -79.5252906 ] ]}";

    @Test
    public void toCSV() throws ResultFormattingException {
        String format = new ResultFormatterCSV().format(CSV_NUMERIC_VALUES);
        assertThat(format, is("\"loc.latitude\",\"loc.longitude\"\n-25.0,135.0\n40.9777996,-79.5252906\n"));
    }

    @Test
    public void toCSVOutputStream() throws ResultFormattingException {
        InputStream is = IOUtils.toInputStream(CSV_NUMERIC_VALUES, StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterCSV().format(is, os);
        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8),
                is("\"loc.latitude\",\"loc.longitude\"\n-25.0,135.0\n40.9777996,-79.5252906\n"));
    }

    @Test
    public void toCSVText() throws ResultFormattingException {
        String format = new ResultFormatterCSV().format(CSV_WITH_TEXT_VALUES);
        assertThat(format, is("\"loc.latitude\",\"loc.longitude\"\n\"one\",\"two\"\n\"three\",\"four\"\n"));
    }

    @Test
    public void toCSVTextOutputStream() throws ResultFormattingException {
        InputStream is = IOUtils.toInputStream(CSV_WITH_TEXT_VALUES, StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterCSV().format(is, os);
        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8),
                is("\"loc.latitude\",\"loc.longitude\"\n\"one\",\"two\"\n\"three\",\"four\"\n"));
    }


    @Test
    public void toCSVQuotedText() throws ResultFormattingException {
        String format = new ResultFormatterCSV().format(CSV_WITH_QUOTED_TEXT_VALUES);
        String expectedValue = "\"loc.latitude\",\"loc.longitude\"\n\"and he said: \"\"boo\"\"\",\"two\"\n\"three\",\"four\"\n";
        assertThat(format, is(expectedValue));
    }

    @Test
    public void toCSVQyotedTextOutputStream() throws ResultFormattingException {
        InputStream is = IOUtils.toInputStream(CSV_WITH_QUOTED_TEXT_VALUES, StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterCSV().format(is, os);
        String expectedValue = "\"loc.latitude\",\"loc.longitude\"\n\"and he said: \"\"boo\"\"\",\"two\"\n\"three\",\"four\"\n";
        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8),
                is(expectedValue));
    }

}
