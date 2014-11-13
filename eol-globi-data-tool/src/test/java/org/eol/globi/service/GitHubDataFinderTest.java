package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class GitHubDataFinderTest {

    public static final String TEMPLATE_DATA_REPOSITORY = "globalbioticinteractions/template-dataset";

    @Test
    public void discoverRepos() throws IOException, URISyntaxException {
        List<String> reposWithData = GitHubDataFinder.find();
        assertThat(reposWithData, hasItem(TEMPLATE_DATA_REPOSITORY));
    }

    @Test
    public void findFile() throws IOException, URISyntaxException {
        assertThat(GitHubDataFinder.hasInteractionData(TEMPLATE_DATA_REPOSITORY), is(true));
    }

    @Test
    public void fileNotFound() throws IOException {
        assertThat(GitHubDataFinder.hasInteractionData("ropensci/rgbif"), is(false));
    }

}
