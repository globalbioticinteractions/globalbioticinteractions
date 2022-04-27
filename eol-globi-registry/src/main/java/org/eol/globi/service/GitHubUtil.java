package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.util.HttpUtil;
import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.util.GitClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GitHubUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubUtil.class);

    private static String httpGet(ResponseHandler<String> responseHandler, URI githubURI) throws URISyntaxException, IOException {
        HttpClientBuilder httpClientBuilder = HttpUtil.createHttpClientBuilder(HttpUtil.TIMEOUT_SHORT);
        return doHttpGet(
                httpClientBuilder,
                responseHandler, githubURI);
    }

    static void doHttpGet(HttpClientBuilder httpClientBuilder, URI githubURI
    ) throws URISyntaxException, IOException {
        doHttpGet(httpClientBuilder, new BasicResponseHandler(), githubURI);
    }

    private static String doHttpGet(HttpClientBuilder httpClientBuilder,
                                    ResponseHandler<String> responseHandler,
                                    URI githubURI) throws IOException {
        CloseableHttpClient build = httpClientBuilder.build();
        URI requestUrl = githubURI;
        return HttpUtil.executeAndRelease(new HttpGet(requestUrl), build, responseHandler);
    }

    public static URI getGitHubAPIEndpoint(String path, String query) throws URISyntaxException {
        return new URI("https", null, "api.github.com", -1, path, query, null);
    }

    private static boolean hasInteractionData(String repoName, String globiFilename, String commitHash) throws IOException {
        HttpHead request = new HttpHead(getBaseUrl(repoName, commitHash) + "/" + globiFilename);
        try {
            HttpResponse execute = HttpUtil.getHttpClient().execute(request);
            return execute.getStatusLine().getStatusCode() == 200;
        } finally {
            request.releaseConnection();
        }
    }

    static List<String> find() throws URISyntaxException, IOException {
        return find(inStream -> inStream);
    }

    public static List<String> find(InputStreamFactory inputStreamFactory) throws URISyntaxException, IOException {
        List<Pair<String, String>> globiRepos = searchGitHubForCandidateRepositories(inputStreamFactory);

        List<String> reposWithData = new ArrayList<String>();
        for (Pair<String, String> globiRepo : globiRepos) {
            if (isGloBIRepository(globiRepo.getKey(), globiRepo.getValue())) {
                reposWithData.add(globiRepo.getKey());
            }
        }
        return reposWithData;
    }

    private static List<Pair<String, String>> searchGitHubForCandidateRepositories(InputStreamFactory inputStreamFactory) throws URISyntaxException, IOException {
        int page = 1;
        int totalAvailable = 0;
        List<Pair<String, String>> globiRepos = new ArrayList<>();
        do {
            LOG.info("searching for repositories that mention [globalbioticinteractions], page [" + page + "]...");
            String query = "q=globalbioticinteractions+in:readme+fork:true" +
                    "&per_page=100" +
                    "&page=" + page;
            String repositoriesThatMentionGloBI
                    = httpGet(
                    new ResponseHandlerWithInputStreamFactory(inputStreamFactory), getGitHubAPIEndpoint("/search/repositories", query));
            JsonNode jsonNode = new ObjectMapper().readTree(repositoriesThatMentionGloBI);
            if (jsonNode.has("total_count")) {
                totalAvailable = jsonNode.get("total_count").asInt();
            }
            if (jsonNode.has("items")) {
                for (JsonNode item : jsonNode.get("items")) {
                    if (item.has("full_name")) {
                        String repoName = item.get("full_name").asText();
                        String branch = item.get("default_branch").asText();
                        globiRepos.add(Pair.of(repoName, branch));
                    }
                }
            }
            page++;
        }
        while (globiRepos.size() < totalAvailable);
        LOG.info("searching for repositories that mention [globalbioticinteractions] done.");
        return globiRepos;
    }

    static boolean isGloBIRepository(String globiRepo, String commitSHA) throws IOException {
        return hasInteractionData(globiRepo, "globi.json", commitSHA)
                || hasInteractionData(globiRepo, "globi-dataset.jsonld", commitSHA);
    }

    static String lastCommitSHA(String repository) throws IOException, URISyntaxException {
        return lastCommitSHA(repository, inputStream -> inputStream);
    }

    public static String lastCommitSHA(String repository, InputStreamFactory inputStreamFactory) throws IOException, URISyntaxException {
        return GitClient.getLastCommitSHA1("https://github.com/" + repository, new ResponseHandlerWithInputStreamFactory(inputStreamFactory));
    }

    private static String getPropertyOrEnvironmentVariable(String environmentVariableName, String javaPropertyName) {
        String environmentVariable = System.getenv(environmentVariableName);
        String property = System.getProperty(javaPropertyName, environmentVariable);
        if (StringUtils.isBlank(property)) {
            LOG.warn("Please set java property [" + javaPropertyName + "] or environment variable [" + environmentVariableName + "] to avoid GitHub API rate limits");
        }
        return property;
    }

    private static String getBaseUrl(String repo, String lastCommitSHA) {
        return "https://raw.githubusercontent.com/" + repo + "/" + lastCommitSHA;
    }

    public static String getBaseUrlLastCommit(String repo, InputStreamFactory is) throws IOException, URISyntaxException {
        String lastCommitSHA = lastCommitSHA(repo, is);
        if (lastCommitSHA == null) {
            throw new IOException("failed to import github repo [" + repo + "]: no commits found.");
        }
        return getBaseUrl(repo, lastCommitSHA);
    }

    public static Dataset getArchiveDataset(String namespace, String commitSha, InputStreamFactory inputStreamFactory) {
        return new DatasetImpl(
                namespace,
                URI.create("https://github.com/" + namespace + "/archive/" + commitSha + ".zip"),
                inputStreamFactory);
    }

}
