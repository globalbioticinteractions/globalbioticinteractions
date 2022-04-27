package org.globalbioticinteractions.util;

import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GitClientTest {

    @Test
    public void getLastCommit() throws IOException {
        String sha1Hash = GitClient.getLastCommitSHA1("https://github.com/globalbioticinteractions/vertnet", new ResourceService() {

            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return ResourceUtil.asInputStream(resourceName, is -> is);
            }
        });
        assertThat(sha1Hash.length(), is(40));
        assertThat(sha1Hash.matches("[a-z0-9]*"), is(true));
    }


}