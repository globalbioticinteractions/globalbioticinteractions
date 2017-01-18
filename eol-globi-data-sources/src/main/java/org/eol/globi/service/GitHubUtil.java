package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GitHubUtil {
    private static final Log LOG = LogFactory.getLog(GitHubUtil.class);

    private static String httpGet(String path, String query) throws URISyntaxException, IOException {
        return HttpUtil.getContent(new URI("https", null, "api.github.com", -1, path, appendAuth(query), null));
    }

    private static boolean hasInteractionData(String repoName, String globiFilename) throws IOException {
        HttpHead request = new HttpHead(getBaseUrlMaster(repoName) + "/" + globiFilename);
        try {
            HttpResponse execute = HttpUtil.getHttpClient().execute(request);
            return execute.getStatusLine().getStatusCode() == 200;
        } finally {
            request.releaseConnection();
        }
    }

    public static String getBaseUrlMaster(String repoName) {
        return getBaseUrl(repoName, "master");
    }

    public static List<String> find() throws URISyntaxException, IOException {
        int page = 1;
        int totalAvailable = 0;
        List<String> globiRepos = new ArrayList<String>();
        do {
            LOG.info("searching for repositories that mention [globalbioticinteractions], page [" + page + "]...");
            String repositoriesThatMentionGloBI = httpGet("/search/repositories", "q=globalbioticinteractions+in:readme+fork:true&page=" + page);
            JsonNode jsonNode = new ObjectMapper().readTree(repositoriesThatMentionGloBI);
            if (jsonNode.has("total_count")) {
                totalAvailable = jsonNode.get("total_count").getIntValue();
            }
            if (jsonNode.has("items")) {
                for (JsonNode item : jsonNode.get("items")) {
                    if (item.has("full_name")) {
                        globiRepos.add(item.get("full_name").asText());
                    }
                }
            }
            page++;
        }
        while (globiRepos.size() < totalAvailable);
        LOG.info("searching for repositories that mention [globalbioticinteractions] done.");

        List<String> reposWithData = new ArrayList<String>();
        for (String globiRepo : globiRepos) {
            if (isGloBIRepository(globiRepo)) {
                reposWithData.add(globiRepo);
            }
        }
        return reposWithData;
    }

    static boolean isGloBIRepository(String globiRepo) throws IOException {
        return hasInteractionData(globiRepo, "globi.json") || hasInteractionData(globiRepo, "globi-dataset.jsonld");
    }

    static String lastCommitSHA(String repository) throws IOException, URISyntaxException {
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

    private static String appendAuth(String query) {
        String propertyNameGitHubClientId = "github.client.id";
        String propertyNameGitHubClientSecret = "github.client.secret";
        String clientId = System.getProperty(propertyNameGitHubClientId);
        String clientSecret = System.getProperty(propertyNameGitHubClientSecret);

        if (StringUtils.isBlank(clientId)) {
            LOG.warn("variable [" + propertyNameGitHubClientId + "] is not set: this lowers the rate limits");
        } else  if (StringUtils.isBlank(clientSecret)) {
            LOG.warn("variable [" + propertyNameGitHubClientSecret + "] is not set: this lowers the rate limits");
        } else {
            String auth = "client_id=" + clientId + "&client_secret=" + clientSecret;
            query = StringUtils.isBlank(query) ? auth : query + "&" + auth;
        }
        return query;
    }

    private static String getBaseUrl(String repo, String lastCommitSHA) {
        return "https://raw.githubusercontent.com/" + repo + "/" + lastCommitSHA;
    }

    public static String getBaseUrlLastCommit(String repo) throws IOException, URISyntaxException, StudyImporterException {
        String lastCommitSHA = lastCommitSHA(repo);
        if (lastCommitSHA == null) {
            throw new StudyImporterException("failed to import github repo [" + repo + "]: no commits found.");
        }
        return getBaseUrl(repo, lastCommitSHA);
    }

    public static Dataset getArchiveDataset(String namespace, String commitSha) {
        return new DatasetImpl(namespace, URI.create("https://github.com/" + namespace + "/archive/" + commitSha + ".zip"));
    }

    public static void configureStudyWithNamespace(StudyImpl study, boolean shouldResolveReferences, String namespace) {
        study.setSourceId("globi:" + namespace);

        DatasetImpl originatingDataset = new DatasetImpl(namespace, URI.create(getBaseUrlMaster(namespace)));
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(DatasetConstant.SHOULD_RESOLVE_REFERENCES, shouldResolveReferences);
        originatingDataset.setConfig(objectNode);
        study.setOriginatingDataset(originatingDataset);
    }
}
