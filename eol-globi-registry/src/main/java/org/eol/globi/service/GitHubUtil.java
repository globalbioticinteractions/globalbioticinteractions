package org.eol.globi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.util.GitClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GitHubUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubUtil.class);

    private static String retrieveAsString(URI githubURI, ResourceService resourceService) throws URISyntaxException, IOException {
        try (InputStream is = resourceService.retrieve(githubURI)) {
            return org.apache.commons.io.IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    public static URI getGitHubAPIEndpoint(String path, String query) throws URISyntaxException {
        return new URI(
                "https",
                null,
                "api.github.com",
                -1,
                path,
                query,
                null
        );
    }

    private static boolean hasInteractionData(URI gloBIConfigURI, ResourceService resourceService) throws IOException {
        try (InputStream is = resourceService.retrieve(gloBIConfigURI)) {
            IOUtils.copy(is, NullOutputStream.NULL_OUTPUT_STREAM);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    private static URI getGloBIConfigURI(String repoName, String globiFilename, String commitHash) {
        return URI.create(getBaseUrl(repoName, commitHash) + "/" + globiFilename);
    }

    public static List<String> find(ResourceService resourceService) throws URISyntaxException, IOException {
        List<Pair<String, String>> globiRepos = searchGitHubForCandidateRepositories(resourceService);

        List<String> reposWithData = new ArrayList<>();
        for (Pair<String, String> globiRepo : globiRepos) {
            if (isGloBIRepository(globiRepo.getKey(), globiRepo.getValue(), resourceService)) {
                reposWithData.add(globiRepo.getKey());
            }
        }
        return reposWithData;
    }

    private static List<Pair<String, String>> searchGitHubForCandidateRepositories(ResourceService resourceService) throws URISyntaxException, IOException {
        int page = 1;
        int totalAvailable = 0;
        List<Pair<String, String>> globiRepos = new ArrayList<>();
        do {
            LOG.info("searching for repositories that mention [globalbioticinteractions], page [" + page + "]...");
            String query = "q=globalbioticinteractions+in:readme+fork:true" +
                    "&per_page=100" +
                    "&page=" + page;
            String repositoriesThatMentionGloBI
                    = retrieveAsString(
                    getGitHubAPIEndpoint("/search/repositories", query),
                    resourceService
            );
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

    static boolean isGloBIRepository(String globiRepo, String commitSHA, ResourceService resourceService) throws IOException {
        return hasInteractionData(getGloBIConfigURI(globiRepo, "globi.json", commitSHA), resourceService)
                || hasInteractionData(getGloBIConfigURI(globiRepo, "globi-dataset.jsonld", commitSHA), resourceService)
                || hasInteractionData(getGloBIConfigURI(globiRepo, "eml.xml", commitSHA), resourceService);
    }

    public static String lastCommitSHA(String repository, ResourceService resourceService) throws IOException {
        return GitClient.getLastCommitSHA1("https://github.com/" + repository, resourceService);
    }

    private static String getBaseUrl(String repo, String lastCommitSHA) {
        return "https://raw.githubusercontent.com/" + repo + "/" + lastCommitSHA;
    }

    public static String getBaseUrlLastCommit(String repo, ResourceService resourceService) throws IOException, URISyntaxException {
        String lastCommitSHA = lastCommitSHA(repo, resourceService);
        if (lastCommitSHA == null) {
            throw new IOException("failed to import github repo [" + repo + "]: no commits found.");
        }
        return getBaseUrl(repo, lastCommitSHA);
    }

    public static Dataset getArchiveDataset(String namespace, String commitSha, ResourceService resourceService) {
        return new DatasetImpl(
                namespace,
                resourceService,
                URI.create("https://github.com/" + namespace + "/archive/" + commitSha + ".zip")
        );
    }

}
