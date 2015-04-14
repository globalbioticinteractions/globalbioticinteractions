package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class GitHubUtilIT {

    public static final String TEMPLATE_DATA_REPOSITORY_TSV = "globalbioticinteractions/template-dataset";
    public static final String TEMPLATE_DATA_REPOSITORY_JSONLD = "globalbioticinteractions/jsonld-template-dataset";

    @Test
    public void discoverRepos() throws IOException, URISyntaxException {
        List<String> reposWithData = GitHubUtil.find();
        assertThat(reposWithData, hasItem(TEMPLATE_DATA_REPOSITORY_TSV));
        assertThat(reposWithData, hasItem(TEMPLATE_DATA_REPOSITORY_JSONLD));
    }

    @Test
    public void findMostRecentCommit() throws IOException, URISyntaxException {
        String sha = GitHubUtil.lastCommitSHA(GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV);
        assertThat(sha, is(notNullValue()));
    }

}
