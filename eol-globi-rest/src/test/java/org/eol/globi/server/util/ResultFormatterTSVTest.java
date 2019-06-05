package org.eol.globi.server.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResultFormatterTSVTest {

    public static final String WITH_QUOTED_TEXT_VALUES = "{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ \"and he said: \\\"boo\\\"\", \"two\" ], [ \"three\", \"four\" ] ]}";
    public static final String WITH_STRING_VALUES = "{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ \"one\", \"two\" ], [ \"three\", \"four\" ] ]}";
    public static final String WITH_NUMERIC_VALUES = "{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ -25.0, 135.0 ], [ 40.9777996, -79.5252906 ] ]}";

    @Test
    public void toTSV() throws ResultFormattingException {
        String format = new ResultFormatterTSV().format(WITH_NUMERIC_VALUES);
        assertThat(format, is("loc.latitude\tloc.longitude\n-25.0\t135.0\n40.9777996\t-79.5252906\n"));
    }

    @Test
    public void toTSVStream() throws ResultFormattingException {
        assertResults(WITH_NUMERIC_VALUES,
                "loc.latitude\tloc.longitude\n-25.0\t135.0\n40.9777996\t-79.5252906");
    }

    @Test
    public void toTSVText() throws ResultFormattingException {
        String format = new ResultFormatterTSV().format(WITH_STRING_VALUES);
        assertThat(format, is("loc.latitude\tloc.longitude\none\ttwo\nthree\tfour\n"));
    }

    @Test
    public void toTSVTextStream() throws ResultFormattingException {
        assertResults(WITH_STRING_VALUES,
                "loc.latitude\tloc.longitude\none\ttwo\nthree\tfour");
    }

    @Test
    public void toTSVQuotedText() throws ResultFormattingException {
        String format = new ResultFormatterTSV().format(WITH_QUOTED_TEXT_VALUES);
        String expectedValues = "loc.latitude\tloc.longitude\nand he said: \"boo\"\ttwo\nthree\tfour\n";
        assertThat(format, is(expectedValues));
    }

    @Test
    public void toTSVQuotedTextOutputStream() throws ResultFormattingException {
        assertResults(WITH_QUOTED_TEXT_VALUES,
                "loc.latitude\tloc.longitude\nand he said: \"boo\"\ttwo\nthree\tfour");
    }

    private void assertResults(String inputText, String expectedValues) throws ResultFormattingException {
        InputStream is = IOUtils.toInputStream(inputText, StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterTSV().format(is, os);
        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8),
                is(expectedValues));
    }


}
