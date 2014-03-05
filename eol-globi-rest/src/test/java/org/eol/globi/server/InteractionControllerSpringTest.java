package org.eol.globi.server;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class InteractionControllerSpringTest extends SpringTestBase {

    @Autowired
    private InteractionController controller;

    @Test
    public void findPredatorDistinctDOT() throws IOException, URISyntaxException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(new HashMap<String, String>() {{
            put("type", "dot");
        }});
        String list = controller.findInteractions(request);
        assertThat(list, is(notNullValue()));

    }
}
