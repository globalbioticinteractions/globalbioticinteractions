package org.eol.globi.service;

import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.eol.globi.util.ResourceUtil;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class GitHubUtilIT {

    static final String TEMPLATE_DATA_REPOSITORY_TSV = "globalbioticinteractions/template-dataset";
    private static final String TEMPLATE_DATA_REPOSITORY_JSONLD = "globalbioticinteractions/jsonld-template-dataset";

    @Test
    public void discoverRepos() throws IOException, URISyntaxException {
        List<String> reposWithData = GitHubUtil.find(new ResourceServiceHTTP(new InputStreamFactoryNoop()));
        assertThat(reposWithData, CoreMatchers.hasItem(TEMPLATE_DATA_REPOSITORY_TSV));
        assertThat(reposWithData, CoreMatchers.hasItem(TEMPLATE_DATA_REPOSITORY_JSONLD));
    }

   @Test
    public void isGloBIRepo() throws IOException {
       ResourceServiceHTTP resourceService = new ResourceServiceHTTP(new InputStreamFactoryNoop());

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
                new ResourceServiceHTTP(new InputStreamFactoryNoop())
        );
        assertThat(sha, is(notNullValue()));
    }


}
