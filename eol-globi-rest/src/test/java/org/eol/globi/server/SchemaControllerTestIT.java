package org.eol.globi.server;

import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.when;

public class SchemaControllerTestIT {

    @Test
    public void findSupportedInteractionTypes() throws IOException, URISyntaxException {
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
    public void findSupportedInteractionTypesById() throws IOException, URISyntaxException {
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
    public void findSupportedInteractionTypesBees() throws IOException, URISyntaxException {
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