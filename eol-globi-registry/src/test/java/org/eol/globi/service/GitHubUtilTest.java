package org.eol.globi.service;

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
        String lastCommitSHA = GitHubUtil.lastCommitSHA(GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV, new ResourceService() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return ResourceUtil.asInputStream(resourceName, in -> in);
            }
        });
        assertThat(GitHubUtil.isGloBIRepository(GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV, lastCommitSHA, new ResourceService() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return ResourceUtil.asInputStream(resourceName, is -> is);
            }
        }), is(true));
    }

    @Test
    public void nonGloBIRepo() throws IOException, URISyntaxException {
        String lastCommitSHA = GitHubUtil.lastCommitSHA(GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV, new ResourceService() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return ResourceUtil.asInputStream(resourceName, in -> in);
            }
        });
        assertThat(GitHubUtil.isGloBIRepository("ropensci/rgbif", lastCommitSHA, new ResourceService() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return ResourceUtil.asInputStream(resourceName, is -> is);
            }
        }), is(false));
    }

}
