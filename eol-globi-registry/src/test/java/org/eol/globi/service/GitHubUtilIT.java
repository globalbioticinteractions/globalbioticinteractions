package org.eol.globi.service;

import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class GitHubUtilIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    static final String TEMPLATE_DATA_REPOSITORY_TSV = "globalbioticinteractions/template-dataset";
    private static final String TEMPLATE_DATA_REPOSITORY_JSONLD = "globalbioticinteractions/jsonld-template-dataset";

    @Test
    public void discoverRepos() throws IOException, URISyntaxException {
        List<String> reposWithData = GitHubUtil.find(getResourceService());
        assertThat(reposWithData, CoreMatchers.hasItem(TEMPLATE_DATA_REPOSITORY_TSV));
        assertThat(reposWithData, CoreMatchers.hasItem(TEMPLATE_DATA_REPOSITORY_JSONLD));
    }

    private ResourceServiceHTTP getResourceService() throws IOException {
        return new ResourceServiceHTTP(new InputStreamFactoryNoop(), folder.newFolder());
    }

    @Test
    public void isGloBIRepo() throws IOException {
       ResourceServiceHTTP resourceService = getResourceService();

       String repoName = "globalbioticinteractions/carvalheiro2023";
       String sha = GitHubUtil.lastCommitSHA(
               repoName,
               resourceService
       );
       assertTrue(GitHubUtil.isGloBIRepository(repoName, sha, resourceService));
    }

    @Test
    public void findMostRecentCommit() throws IOException {
        String sha = GitHubUtil.lastCommitSHA(
                GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV,
                getResourceService()
        );
        assertThat(sha, is(notNullValue()));
    }


}
