package org.eol.globi.server;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class ReportControllerTest {

    @Test
    public void sources() throws IOException {
        String studies = new CypherQueryExecutor(new ReportController().sourcesNew()).execute(null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(studies);
        assertThat(jsonNode.has("data"), is(true));
        JsonNode data = jsonNode.get("data");
        assertThat(data.get(0).size() > 0, is(true));
        assertThat(data.get(0).get(0).getTextValue(), not(is(nullValue())));
    }


    @Test
    public void info() throws IOException {
        String studies = new CypherQueryExecutor(new ReportController().infoNew(null)).execute(null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(studies);
        assertThat(jsonNode.has("data"), is(true));
        JsonNode data = jsonNode.get("data");
        assertThat(data.get(0).get(0).getIntValue() > 0, is(true));
    }

    @Test
    public void infoBySource() throws IOException {
        String studies = new CypherQueryExecutor(new ReportController().infoNew("http://gomexsi.tamucc.edu")).execute(null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(studies);
        assertThat(jsonNode.has("data"), is(true));
        JsonNode data = jsonNode.get("data");
        assertThat(data.get(0).get(0).getIntValue() > 0, is(true));
    }

    @Test
    public void infoBySourceAndBoundingBox() throws IOException {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getParameterMap()).thenReturn(new HashMap<String, String[]>() {{
            put("source", new String[] {"http://gomexsi.tamucc.edu"});
            put("bbox", new String[]{"-100.0,0.0,-80.08,40.32"});
        }
        });
        String response = new CypherQueryExecutor(new ReportController().spatialInfoNew(req)).execute(req);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response);
        assertThat(jsonNode.has("data"), is(true));
        JsonNode data = jsonNode.get("data");
        assertThat(data.get(0).get(0).getIntValue() > 0, is(true));
    }

    @Test
    public void spatialInfo() throws IOException {
        String studies = new CypherQueryExecutor(new ReportController().infoNew(null)).execute(null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(studies);
        assertThat(jsonNode.has("data"), is(true));
        JsonNode data = jsonNode.get("data");
        assertThat(data.get(0).get(0).getIntValue() > 0, is(true));
    }
}
