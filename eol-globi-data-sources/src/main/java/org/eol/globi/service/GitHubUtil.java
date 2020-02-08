package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.util.HttpUtil;
import org.eol.globi.util.InputStreamFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class GitHubUtil {
    private static final Log LOG = LogFactory.getLog(GitHubUtil.class);

    public static final String GITHUB_CLIENT_ID_JAVA_PROPERTY_NAME = "github.client.id";
    public static final String GITHUB_CLIENT_SECRET_JAVA_PROPERTY_NAME = "github.client.secret";
    public static final String GITHUB_CLIENT_ID_ENVIRONMENT_VARIABLE = "GITHUB_CLIENT_ID";
    public static final String GITHUB_CLIENT_SECRET_ENVIRONMENT_VARIABLE = "GITHUB_CLIENT_SECRET";

    static String httpGet(String path, String query) throws URISyntaxException, IOException {
        return httpGet(path, query, new BasicResponseHandler());
    }

    static String httpGet(String path, String query, ResponseHandler<String> responseHandler) throws URISyntaxException, IOException {
        HttpClientBuilder httpClientBuilder = HttpUtil.createHttpClientBuilder(HttpUtil.FIVE_SECONDS);
        return doHttpGetWithBasicAuthIfCredentialsIfAvailable(
                path,
                query,
                httpClientBuilder,
                getGitHubClientId(),
                getGitHubClientSecret(),
                responseHandler);
    }

    static String doHttpGetWithBasicAuthIfCredentialsIfAvailable(String path,
                                                                 String query,
                                                                 HttpClientBuilder httpClientBuilder,
                                                                 String id,
                                                                 String secret) throws URISyntaxException, IOException {
        return doHttpGetWithBasicAuthIfCredentialsIfAvailable(path, query, httpClientBuilder, id, secret, new BasicResponseHandler());
    }

    static String doHttpGetWithBasicAuthIfCredentialsIfAvailable(String path,
                                                                 String query,
                                                                 HttpClientBuilder httpClientBuilder,
                                                                 String id,
                                                                 String secret,
                                                                 ResponseHandler<String> responseHandler) throws URISyntaxException, IOException {
        CloseableHttpClient build = httpClientBuilder.build();

        URI requestUrl = new URI("https", null, "api.github.com", -1, path, query, null);

        HttpGet request = StringUtils.isNotBlank(id) && StringUtils.isNotBlank(secret)
                ? HttpUtil.withBasicAuthHeader(new HttpGet(requestUrl), id, secret)
                : new HttpGet(requestUrl);

        return HttpUtil.executeAndRelease(request, build, responseHandler);
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
        return find(inStream -> inStream);
    }

    public static List<String> find(InputStreamFactory inputStreamFactory) throws URISyntaxException, IOException {
        List<String> globiRepos = searchGitHubForCandidateRepositories(inputStreamFactory);


        List<String> reposWithData = new ArrayList<String>();
        for (String globiRepo : globiRepos) {
            if (isGloBIRepository(globiRepo)) {
                reposWithData.add(globiRepo);
            }
        }
        return reposWithData;
    }

    public static List<String> searchGitHubForCandidateRepositories(InputStreamFactory inputStreamFactory) throws URISyntaxException, IOException {
        int page = 1;
        int totalAvailable = 0;
        List<String> globiRepos = new ArrayList<String>();
        do {
            LOG.info("searching for repositories that mention [globalbioticinteractions], page [" + page + "]...");
            String query = "q=globalbioticinteractions+in:readme+fork:true" +
                    "&per_page=100" +
                    "&page=" + page;
            String repositoriesThatMentionGloBI
                    = httpGet("/search/repositories",
                    query,
                    new BasicResponseHandler() {
                        @Override
                        public String handleEntity(final HttpEntity entity) throws IOException {
                            HttpEntityProxy httpEntityProxy = new HttpEntityProxy(entity, inputStreamFactory);
                            return EntityUtils.toString(httpEntityProxy);
                        }
                    });
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
        return globiRepos;
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

    private static String getGitHubClientSecret() {
        return getPropertyOrEnvironmentVariable(
                GITHUB_CLIENT_SECRET_ENVIRONMENT_VARIABLE,
                GITHUB_CLIENT_SECRET_JAVA_PROPERTY_NAME);
    }

    private static String getPropertyOrEnvironmentVariable(String environmentVariableName, String javaPropertyName) {
        String environmentVariable = System.getenv(environmentVariableName);
        String property = System.getProperty(javaPropertyName, environmentVariable);
        if (StringUtils.isBlank(property)) {
            LOG.warn("Please set java property [" + javaPropertyName + "] or environment variable [" + environmentVariableName + "] to avoid GitHub API rate limits");
        }
        return property;
    }

    private static String getGitHubClientId() {
        return getPropertyOrEnvironmentVariable(
                GITHUB_CLIENT_ID_ENVIRONMENT_VARIABLE,
                GITHUB_CLIENT_ID_JAVA_PROPERTY_NAME);
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

    public static Dataset getArchiveDataset(String namespace, String commitSha, InputStreamFactory inputStreamFactory) {
        return new DatasetImpl(namespace, URI.create("https://github.com/" + namespace + "/archive/" + commitSha + ".zip"), inputStreamFactory);
    }

    public static void configureStudyWithNamespace(StudyImpl study, boolean shouldResolveReferences, String namespace) {
        study.setSourceId("globi:" + namespace);

        DatasetImpl originatingDataset = new DatasetImpl(namespace, URI.create(getBaseUrlMaster(namespace)), inStream -> inStream);
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put(DatasetConstant.SHOULD_RESOLVE_REFERENCES, shouldResolveReferences);
        originatingDataset.setConfig(objectNode);
        study.setOriginatingDataset(originatingDataset);
    }

    private static class HttpEntityProxy implements HttpEntity {

        private final HttpEntity entity;
        private final InputStreamFactory inputStreamFactory;

        public HttpEntityProxy(HttpEntity entity, InputStreamFactory inputStreamFactory) {
            this.entity = entity;
            this.inputStreamFactory = inputStreamFactory;
        }

        @Override
        public boolean isRepeatable() {
            return entity.isRepeatable();
        }

        @Override
        public boolean isChunked() {
            return entity.isChunked();
        }

        @Override
        public long getContentLength() {
            return entity.getContentLength();
        }

        @Override
        public Header getContentType() {
            return entity.getContentType();
        }

        @Override
        public Header getContentEncoding() {
            return entity.getContentEncoding();
        }

        @Override
        public InputStream getContent() throws IOException, UnsupportedOperationException {
            return inputStreamFactory.create(entity.getContent());
        }

        @Override
        public void writeTo(OutputStream outputStream) throws IOException {
            entity.writeTo(outputStream);
        }

        @Override
        public boolean isStreaming() {
            return entity.isStreaming();
        }

        @Override
        public void consumeContent() throws IOException {
            entity.consumeContent();
        }
    }
}
