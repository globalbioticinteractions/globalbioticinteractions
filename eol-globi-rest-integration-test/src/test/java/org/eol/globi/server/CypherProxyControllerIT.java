package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.HttpUtil;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

public class CypherProxyControllerIT extends ITBase {

    @Test
    public void findExternalUrl() throws IOException {
        String uri = getURLPrefix() + "findExternalUrlForTaxon/Homo%20sapiens";
        assertThat(HttpUtil.getRemoteJson(uri), containsString("url"));
    }

    @Test
    public void getGoMexSILocations() throws IOException {
        String uri = getURLPrefix() + "locations?accordingTo=gomexsi";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void getByCitation() throws IOException {
        String uri = getURLPrefix() + "interaction?accordingTo=Smales%2C%20L%20R.%20%22An%20Annotated%20Checklist%20of%20the%20Australian%20Acanthocephala%20from%20Mammalian%20and%20Bird%20Hosts.%22%20Records%20of%20the%20South%20Australian%20Museum%2036%20%282003%29%3A%2059-82.";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void getByShortCitation() throws IOException {
        String uri = getURLPrefix() + "interaction?accordingTo=gomexsi";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void getAllLocations() throws IOException {
        String uri = getURLPrefix() + "locations";
        String response = HttpUtil.getRemoteJson(uri);
        assertThat(response, is(not(nullValue())));
    }

    @Test
    public void findShortestPaths() throws IOException {
        CypherQuery cypherQuery = new CypherProxyController().findShortestPathsNew(null, "Homo sapiens", "Rattus rattus");
        String externalLink = new CypherQueryExecutor(cypherQuery).execute(null);
        assertThat(externalLink, CoreMatchers.containsString("Rattus rattus"));
    }

    @Test
    public void findExternalLinkFoStudyWithTitle() throws IOException {
        String externalLink = new CypherProxyController().findExternalLinkForStudyWithTitle(null, "bioinfo:ref:147884");
        assertThat(externalLink, is("{\"url\":\"http://bioinfo.org.uk/html/b147884.htm\"}"));
    }

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
