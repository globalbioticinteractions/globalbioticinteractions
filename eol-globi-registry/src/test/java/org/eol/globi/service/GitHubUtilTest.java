package org.eol.globi.service;

import org.eol.globi.util.ResourceServiceHTTP;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;

public class GitHubUtilTest {

    @Test
    public void isGloBIRepo() throws IOException {
        String repo = GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV;
        ResourceService resourceService = new ResourceServiceHTTP(is -> is);
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
        ResourceService resourceService = new ResourceServiceHTTP(is -> is);

        String lastCommitSHA = GitHubUtil.lastCommitSHA(
                repo,
                resourceService
        );

        assertThat(GitHubUtil.isGloBIRepository(repo, lastCommitSHA, resourceService),
                is(false));
    }

}
