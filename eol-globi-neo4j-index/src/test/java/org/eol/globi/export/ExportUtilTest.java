package org.eol.globi.export;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ExportUtilTest {

    @Test
    public void appendRow() throws IOException {
        Iterable<Map<String, Object>> rows = Collections.singletonList(new HashMap<String, Object>() {{
            put("oneKey", "o\t\tn\ne");
            put("twoKey", "two\n");
            put("threeKey", "three\r\n\t");
        }});
        StringWriter writer = new StringWriter();
        ExportUtil.TsvValueJoiner joiner = new ExportUtil.TsvValueJoiner();
        ExportUtil.appendRow(ExportUtil.AppenderWriter.of(writer, joiner), rows, Arrays.asList("oneKey", "twoKey", "threeKey"));
        assertThat(writer.getBuffer().toString(), is("\no n e\ttwo\tthree"));
    }

    @Test
    public void appendRowCSV() throws IOException {
        Iterable<Map<String, Object>> rows = Collections.singletonList(new HashMap<String, Object>() {{
            put("oneKey", "o\"n\ne");
            put("twoKey", "two");
            put("threeKey", "three\r\n\t");
        }});
        StringWriter writer = new StringWriter();
        ExportUtil.ValueJoiner csvJoiner = new ExportUtil.CsvValueJoiner();
        ExportUtil.appendRow(ExportUtil.AppenderWriter.of(writer, csvJoiner), rows, Arrays.asList("oneKey", "twoKey", "threeKey"));
        assertThat(writer.getBuffer().toString(), is("\n\"o\"\"n\ne\",two,\"three\r\n\t\""));
    }

}