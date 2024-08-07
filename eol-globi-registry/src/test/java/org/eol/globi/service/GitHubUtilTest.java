package org.eol.globi.service;

import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class GitHubUtilTest {

    @Rule

    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void isGloBIRepo() throws IOException {
        String repo = GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV;
        ResourceService resourceService = getResourceService();
        String lastCommitSHA = GitHubUtil.lastCommitSHA(
                repo,
                resourceService
        );

        assertThat(GitHubUtil.isGloBIRepository(repo,
                lastCommitSHA,
                resourceService),
                is(true));
    }

    @Test
    public void nonGloBIRepo() throws IOException {
        String repo = "ropensci/rgbif";
        ResourceService resourceService = getResourceService();

        String lastCommitSHA = GitHubUtil.lastCommitSHA(
                repo,
                resourceService
        );

        assertThat(GitHubUtil.isGloBIRepository(repo, lastCommitSHA, resourceService),
                is(false));
    }

    private ResourceService getResourceService() throws IOException {
        return new ResourceServiceHTTP(new InputStreamFactoryNoop(), folder.newFolder());
    }

}
