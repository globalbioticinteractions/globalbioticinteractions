package org.eol.globi.service;

import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class GitHubUtilTest {

    @Test
    public void findFile() throws IOException, URISyntaxException {
        assertThat(GitHubUtil.hasInteractionData(GitHubUtilIT.TEMPLATE_DATA_REPOSITORY), is(true));
    }

    @Test
    public void fileNotFound() throws IOException {
        assertThat(GitHubUtil.hasInteractionData("ropensci/rgbif"), is(false));
    }



}
