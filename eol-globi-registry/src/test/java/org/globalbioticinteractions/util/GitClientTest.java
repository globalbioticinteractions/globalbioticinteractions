package org.globalbioticinteractions.util;

import org.globalbioticinteractions.util.GitClient;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class GitClientTest {

    @Test
    public void getLastCommit() throws IOException {
        String sha1Hash = GitClient.getLastCommitSHA1("https://github.com/globalbioticinteractions/vertnet");
        assertThat(sha1Hash.length(), is(40));
        assertThat(sha1Hash.matches("[a-z0-9]*"), is(true));
    }


}