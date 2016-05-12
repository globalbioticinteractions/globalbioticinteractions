package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class GitHubUtilTest {

    @Test
    public void isGloBIRepo() throws IOException, URISyntaxException {
        assertThat(GitHubUtil.isGloBIRepository(GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV), is(true));
    }

    @Test
    public void nonGloBIRepo() throws IOException {
        assertThat(GitHubUtil.isGloBIRepository("ropensci/rgbif"), is(false));
    }

@Test
    public void baseUrlMaster() throws IOException {
        assertThat(GitHubUtil.getBaseUrlMaster("globalbioticinteractions/template-dataset"),
                is("https://raw.githubusercontent.com/globalbioticinteractions/template-dataset/master"));
    }



}
