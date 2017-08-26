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
            put("oneKey", null);
            put("twoKey", "two");
            put("threeKey", "\t");
        }});
        StringWriter writer = new StringWriter();
        ExportUtil.appendRow(writer, rows, Arrays.asList("oneKey", "twoKey", "threeKey"));
        assertThat(writer.getBuffer().toString(), is("\n\ttwo\t "));
    }

}