package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

public class CypherProxyControllerTest {

    @Ignore(value = "this assumes an externally running system")
    @Test
    public void findExternalLinkForTaxonWithName() throws IOException {
        String externalLink = new CypherProxyController().findExternalLinkForTaxonWithName(null, "Homo sapiens");
        assertThat(externalLink, is("{\"url\":\"http://eol.org/pages/327955\"}"));
    }

    @Test
    public void findExternalLinkForTaxonWithNonExistingName() throws IOException {
        String externalLink = new CypherProxyController().findExternalLinkForTaxonWithName(null, "This aint exist yet");
        assertThat(externalLink, is("{}"));
    }


    @Test
    public void findExternalLinkForNonExistingStudyWithTitle() throws IOException {
        String externalLink = new CypherProxyController().findExternalLinkForStudyWithTitle(null, "None existing study");
        assertThat(externalLink, is("{}"));
    }

}
