package org.eol.globi.server;

import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.junit.Assert.assertThat;

public class CypherProxyControllerSpringTest extends SpringTestBase {

    @Autowired
    private CypherProxyController controller;


    @Test
    public void findExternalUrlForExternalId() {
        assertThat(controller.findExternalLinkForExternalId("EOL:1235"),
                Is.is("{\"url\":\"http://eol.org/pages/1235\"}"));
    }

    @Ignore(value = "this assumes an externally running system")
    @Test
    public void findExternalUrlForTaxon() throws IOException {
        assertThat(controller.findExternalLinkForTaxonWithName(null, "Homo sapiens"),
                Is.is("{\"url\":\"http://eol.org/pages/327955\"}"));
    }

}
