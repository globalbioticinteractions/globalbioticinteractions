package org.eol.globi.server;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class SchemaControllerSpringTest extends SpringTestBase {

    @Autowired
    private SchemaController controller;


    @Test
    public void retrieveInteractionFields() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String[]>() {
            {
                put("type", new String[]{"dot"});
            }
        });
        String interactions = controller.getInteractionFields(request);

        assertThat(interactions,
                Is.is(IOUtils.toString(getClass().getResourceAsStream("interactionFields.json"), StandardCharsets.UTF_8))
        );
    }

    @Test
    public void retrieveInteractionFieldsCSV() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameter("type")).thenReturn("csv");
        String interactions = controller.getInteractionFields(request);

        assertThat(interactions,
                Is.is(IOUtils.toString(getClass().getResourceAsStream("interactionFields.csv"), StandardCharsets.UTF_8))
        );
    }

}