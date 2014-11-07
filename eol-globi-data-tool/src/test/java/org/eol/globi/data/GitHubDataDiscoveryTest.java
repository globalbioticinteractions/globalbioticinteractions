package org.eol.globi.data;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.HttpUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class GitHubDataDiscoveryTest {

    public static final String TEMPLATE_DATA_REPOSITORY = "globalbioticinteractions/template-dataset";

    @Test
    public void discoverRepos() throws IOException, URISyntaxException {

        String path = "/search/repositories";
        String query = "q=globalbioticinteractions+in:readme";
        String repositoriesThatMentionGloBI = httpGet(path, query);

        List<String> globiRepos = new ArrayList<String>();
        JsonNode jsonNode = new ObjectMapper().readTree(repositoriesThatMentionGloBI);
        if (jsonNode.has("items")) {
            for (JsonNode item : jsonNode.get("items")) {
                if (item.has("full_name")) {
                    globiRepos.add(item.get("full_name").getValueAsText());
                }
            }
        }

        assertThat(globiRepos, hasItem(TEMPLATE_DATA_REPOSITORY));
    }

    @Test
    public void findFile() throws IOException, URISyntaxException {
        assertThat(hasInteractionData(TEMPLATE_DATA_REPOSITORY), is(true));
    }

    @Test
    public void fileNotFound() throws IOException {
        assertThat(hasInteractionData("ropensci/rgbif"), is(false));
    }

    protected boolean hasInteractionData(String repoName) throws IOException {
        HttpResponse execute = HttpUtil.createHttpClient().execute(new HttpHead("https://raw.githubusercontent.com/" + repoName + "/master/.globi.yml"));
        return execute.getStatusLine().getStatusCode() == 200;
    }


    protected String httpGet(String path, String query) throws URISyntaxException, IOException {
        URI uri = new URI("https", null, "api.github.com", 443, path, query, null);
        System.out.println(uri.toURL().toExternalForm());
        return HttpUtil.createHttpClient().execute(
                new HttpGet(uri)
                , new BasicResponseHandler());
    }

}
