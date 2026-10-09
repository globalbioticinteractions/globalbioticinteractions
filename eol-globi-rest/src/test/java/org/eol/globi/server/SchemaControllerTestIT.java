package org.eol.globi.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.util.HttpUtil;
import org.hamcrest.core.IsNot;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.when;

public class SchemaControllerTestIT extends ITBase {

    @Test
    public void interactionTypeFields() throws IOException {
        String uri = getURLPrefix() + "interactionFields";
        String response = HttpUtil.getRemoteJson(uri);
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        assertThat(jsonNode.get("latitude"), is(notNullValue()));
    }

    @Test
    public void observationFields() throws IOException {
        String uri = getURLPrefix() + "observationFields";
        String response = HttpUtil.getRemoteJson(uri);
        JsonNode jsonNode = new ObjectMapper().readTree(response);
        assertThat(jsonNode.get("latitude"), is(notNullValue()));
    }


    @Test
    public void findSupportedInteractionTypes() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("taxon", new String[]{"Phocidae"});
            }
        });
        when(request.getParameter("taxon")).thenReturn("something");
        when(request.getParameter("type")).thenReturn("csv");
        String list = new SchemaController().getInteractionTypes(request);
        assertThat(list, not(containsString("pollinate")));
        assertThat(list, containsString("preysOn"));
    }

    @Test
    public void findSupportedInteractionTypesById() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("taxon", new String[]{"EOL:7666"});
            }
        });
        when(request.getParameter("taxon")).thenReturn("something");
        when(request.getParameter("type")).thenReturn("csv");
        String list = new SchemaController().getInteractionTypes(request);
        assertThat(list, not(containsString("pollinate")));
        assertThat(list, containsString("preysOn"));
    }

    @Test
    public void findSupportedInteractionTypesBees() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("taxon", new String[]{"Apidae"});
            }
        });
        when(request.getParameter("taxon")).thenReturn("something");
        when(request.getParameter("type")).thenReturn("csv");
        String list = new SchemaController().getInteractionTypes(request);
        assertThat(list, containsString("pollinate"));
        assertThat(list, not(containsString("pathogenOf")));
    }

}