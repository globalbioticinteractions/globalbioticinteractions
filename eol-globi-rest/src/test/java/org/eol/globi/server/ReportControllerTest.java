package org.eol.globi.server;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ReportControllerTest {

    @Test
    public void sources() throws IOException {
        String studies = new ReportController().sources();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(studies);
        assertThat(jsonNode.has("data"), is(true));
        JsonNode data = jsonNode.get("data");
        assertThat(data.get(0).size() > 0, is(true));
        assertThat(data.get(0).get(0).getValueAsText(), not(is(nullValue())));
    }


    @Test
    public void info() throws IOException {
        String studies = new ReportController().info(null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(studies);
        assertThat(jsonNode.has("data"), is(true));
        JsonNode data = jsonNode.get("data");
        assertThat(data.get(0).get(0).getValueAsInt() > 0, is(true));
    }

    @Test
    public void infoBySource() throws IOException {
        String studies = new ReportController().info("http://gomexsi.tamucc.edu");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(studies);
        assertThat(jsonNode.has("data"), is(true));
        JsonNode data = jsonNode.get("data");
        assertThat(data.get(0).get(0).getValueAsInt() > 0, is(true));
    }
}
