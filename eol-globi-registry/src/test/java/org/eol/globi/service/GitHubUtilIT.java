package org.eol.globi.service;

import org.apache.http.client.HttpResponseException;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eol.globi.util.HttpUtil;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class GitHubUtilIT {

    public static final String TEMPLATE_DATA_REPOSITORY_TSV = "globalbioticinteractions/template-dataset";
    public static final String TEMPLATE_DATA_REPOSITORY_JSONLD = "globalbioticinteractions/jsonld-template-dataset";

    @Test
    public void discoverRepos() throws IOException, URISyntaxException {
        List<String> reposWithData = GitHubUtil.find();
        assertThat(reposWithData, CoreMatchers.hasItem(TEMPLATE_DATA_REPOSITORY_TSV));
        assertThat(reposWithData, CoreMatchers.hasItem(TEMPLATE_DATA_REPOSITORY_JSONLD));
    }

    @Test
    public void findMostRecentCommit() throws IOException, URISyntaxException {
        String sha = GitHubUtil.lastCommitSHA(GitHubUtilIT.TEMPLATE_DATA_REPOSITORY_TSV);
        assertThat(sha, is(notNullValue()));
    }

    @Test
    public void checkInvalidAuth() throws URISyntaxException {
        HttpClientBuilder httpClientBuilder = HttpUtil.createHttpClientBuilder(HttpUtil.TIMEOUT_SHORT);

        try {
            GitHubUtil.doHttpGetWithBasicAuthIfCredentialsIfAvailable(
                    "/user",
                    "",
                    httpClientBuilder,
                    "foo",
                    "bar");
            fail("expected to throw");
        } catch (IOException ex) {
            assertThat(ex.getCause(), instanceOf(HttpResponseException.class));
            assertThat(((HttpResponseException) ex.getCause()).getStatusCode(), is(401));
        }

    }


    @Ignore("replace with valid auth keys")
    @Test
    public void checkValidAuth() throws URISyntaxException, IOException {
        HttpClientBuilder httpClientBuilder = HttpUtil.createHttpClientBuilder(HttpUtil.TIMEOUT_SHORT);

        GitHubUtil.doHttpGetWithBasicAuthIfCredentialsIfAvailable(
                "/repos/globalbioticinteractions/scan/commits",
                "",
                httpClientBuilder,
                "REPLACE_CLIENT_ID",
                "REPLACE_CLIENT_SECRET");

    }

}
