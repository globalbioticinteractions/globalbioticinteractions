package org.eol.globi.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GitHubUtil {
    protected static String httpGet(String path, String query) throws URISyntaxException, IOException {
        return HttpUtil.getContent(new URI("https", null, "api.github.com", 443, path, query, null));
    }

    protected static boolean hasInteractionData(String repoName, String globiFilename) throws IOException {
        HttpHead request = new HttpHead("https://raw.githubusercontent.com/" + repoName + "/master/" + globiFilename);
        try {
            HttpResponse execute = HttpUtil.getHttpClient().execute(request);
            return execute.getStatusLine().getStatusCode() == 200;
        } finally {
            request.releaseConnection();
        }
    }

    public static List<String> find() throws URISyntaxException, IOException {
        String repositoriesThatMentionGloBI = httpGet("/search/repositories", "q=globalbioticinteractions+in:readme+fork:true");

        List<String> globiRepos = new ArrayList<String>();
        JsonNode jsonNode = new ObjectMapper().readTree(repositoriesThatMentionGloBI);
        if (jsonNode.has("items")) {
            for (JsonNode item : jsonNode.get("items")) {
                if (item.has("full_name")) {
                    globiRepos.add(item.get("full_name").asText());
                }
            }
        }

        List<String> reposWithData = new ArrayList<String>();
        for (String globiRepo : globiRepos) {
            if (isGloBIRepository(globiRepo)) {
                reposWithData.add(globiRepo);
            }
        }
        return reposWithData;
    }

    protected static boolean isGloBIRepository(String globiRepo) throws IOException {
        return hasInteractionData(globiRepo, "globi.json") || hasInteractionData(globiRepo, "globi-dataset.jsonld");
    }

    public static String lastCommitSHA(String repository) throws IOException, URISyntaxException {
        String lastCommitSHA = null;
        String response = httpGet("/repos/" + repository + "/commits", null);
        JsonNode commits = new ObjectMapper().readTree(response);
        if (commits.size() > 0) {
            JsonNode mostRecentCommit = commits.get(0);
            if (mostRecentCommit.has("sha")) {
                lastCommitSHA = mostRecentCommit.get("sha").asText();
            }
        }
        return lastCommitSHA;
    }

    public static String getBaseUrl(String repo, String lastCommitSHA) {
        return "https://raw.githubusercontent.com/" + repo + "/" + lastCommitSHA;
    }

    public static String getBaseUrlLastCommit(String repo) throws IOException, URISyntaxException, StudyImporterException {
        String lastCommitSHA = lastCommitSHA(repo);
        if (lastCommitSHA == null) {
            throw new StudyImporterException("failed to import github repo [" + repo + "]: no commits found.");
        }
        return getBaseUrl(repo, lastCommitSHA);
    }
}
