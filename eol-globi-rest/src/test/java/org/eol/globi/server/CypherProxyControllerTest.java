package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

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

    @Ignore
    @Test
    public void findExternalLinkFoStudyWithTitle() throws IOException {
        String externalLink = new CypherProxyController().findExternalLinkForStudyWithTitle(null, "bioinfo:ref:147884");
        assertThat(externalLink, is("{\"url\":\"http://bioinfo.org.uk/html/b147884.htm\"}"));
    }

    @Test
    public void findShortestPaths() throws IOException {
        CypherQuery cypherQuery = new CypherProxyController().findShortestPathsNew(null, "Homo sapiens", "Rattus rattus");
        String externalLink = new CypherQueryExecutor(cypherQuery).execute(null);
        assertThat(externalLink, containsString("Rattus rattus"));
    }





}
