package org.eol.globi.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class CypherProxyControllerSpringTest extends SpringTestBase {

    private static Log LOG = LogFactory.getLog(CypherProxyControllerSpringTest.class);

    @Autowired
    private CypherProxyController controller;

    @Test
    public void findContributors() throws IOException {
        controller.contributors();
    }

}
