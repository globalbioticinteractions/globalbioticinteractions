package org.eol.globi.export;

import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertThat;

public class ExporterBaseTest {

    @Test
    public void ensureCommeasAreEscaped() throws IOException {
        StringWriter writer = new StringWriter();
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("one", "bunch, of, comma, as");
        properties.put("two", "no commas");
        ExporterBase.writeProperties(writer, properties, new String[]{"one", "two"});
        assertThat(writer.toString(), Is.is("\"bunch, of, comma, as\",no commas"));
    }

}
