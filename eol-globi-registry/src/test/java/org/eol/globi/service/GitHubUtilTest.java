package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;

public class GitHubUtilTest {

    @Test
    public void isGloBIRepo() throws IOException, URISyntaxException {
        String lastCommitSHA = GitHubUtil.lastCommitSHA(GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV, in -> in);
        assertThat(GitHubUtil.isGloBIRepository(GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV, lastCommitSHA), is(true));
    }

    @Test
    public void nonGloBIRepo() throws IOException, URISyntaxException {
        String lastCommitSHA = GitHubUtil.lastCommitSHA(GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV, in -> in);
        assertThat(GitHubUtil.isGloBIRepository("ropensci/rgbif", lastCommitSHA), is(false));
    }

}
