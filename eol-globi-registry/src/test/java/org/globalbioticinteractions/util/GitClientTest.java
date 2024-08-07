package org.globalbioticinteractions.util;

import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class GitClientTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void getLastCommit() throws IOException {
        String sha1Hash = GitClient
                .getLastCommitSHA1(
                        "https://github.com/globalbioticinteractions/vertnet",
                        new ResourceServiceHTTP(new InputStreamFactoryNoop(), folder.newFolder()));
        assertThat(sha1Hash.length(), is(40));
        assertThat(sha1Hash.matches("[a-z0-9]*"), is(true));
    }


}