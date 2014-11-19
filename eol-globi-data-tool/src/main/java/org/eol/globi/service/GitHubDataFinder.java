package org.eol.globi.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GitHubDataFinder {
    protected static String httpGet(String path, String query) throws URISyntaxException, IOException {
        URI uri = new URI("https", null, "api.github.com", 443, path, query, null);
        return HttpUtil.createHttpClient().execute(
                new HttpGet(uri)
                , new BasicResponseHandler());
    }

    protected static boolean hasInteractionData(String repoName) throws IOException {
        HttpResponse execute = HttpUtil.createHttpClient().execute(new HttpHead("https://raw.githubusercontent.com/" + repoName + "/master/globi.json"));
        return execute.getStatusLine().getStatusCode() == 200;
    }

    public static List<String> find() throws URISyntaxException, IOException {
        String repositoriesThatMentionGloBI = httpGet("/search/repositories", "q=globalbioticinteractions+in:readme+fork:true");

        List<String> globiRepos = new ArrayList<String>();
        JsonNode jsonNode = new ObjectMapper().readTree(repositoriesThatMentionGloBI);
        if (jsonNode.has("items")) {
            for (JsonNode item : jsonNode.get("items")) {
                if (item.has("full_name")) {
                    globiRepos.add(item.get("full_name").getValueAsText());
                }
            }
        }

        List<String> reposWithData = new ArrayList<String>();
        for (String globiRepo : globiRepos) {
            if (hasInteractionData(globiRepo)) {
                reposWithData.add(globiRepo);
            }
        }
        return reposWithData;
    }

    public static String lastCommitSHA(String repository) throws IOException, URISyntaxException {
        String lastCommitSHA = null;
        String response = httpGet("/repos/" + repository + "/commits", null);
        JsonNode commits = new ObjectMapper().readTree(response);
        if (commits.size() > 0) {
            JsonNode mostRecentCommit = commits.get(0);
            if (mostRecentCommit.has("sha")) {
                lastCommitSHA = mostRecentCommit.get("sha").getValueAsText();
            }
        }
        return lastCommitSHA;
    }

}
