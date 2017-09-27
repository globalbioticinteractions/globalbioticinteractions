package org.globalbioticinteractions.cache;

import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CacheLocalReadonlyTest {

    @Test
    public void candidateHash() {
        String hashCandidate = CacheLocalReadonly.getHashCandidate(URI.create("file:/bla/1234"), URI.create("file:/bla/"));
        assertThat(hashCandidate, is("1234"));
    }

    @Test
    public void candidateHashInJar() throws IOException, URISyntaxException {
        URI jarURI = URI.create("jar:file:/bla/1234!/globi.json");
        String hashCandidate = CacheLocalReadonly.getHashCandidate(jarURI, URI.create("file:/bla/"));
        assertThat(hashCandidate, is("1234"));
    }

    @Test
    public void jarSourceURI() throws IOException, URISyntaxException {
        URI remoteArchiveURI = URI.create("http://example.com/dataset/whatever.zip");
        URI localResourceURI = URI.create("jar:file:/bla/1234!/globi.json");

        URI remoteResourceURI = CacheLocalReadonly.getRemoteJarURIIfNeeded(remoteArchiveURI, localResourceURI);
        assertThat(remoteResourceURI.toString(), is("jar:http://example.com/dataset/whatever.zip!/globi.json"));
        assertThat(localResourceURI.toString(), is("jar:file:/bla/1234!/globi.json"));
    }

   @Test
    public void jarInJar() throws IOException, URISyntaxException {
        URI remoteArchiveURI = URI.create("jar:file://example.com/dataset/whatever.zip!/bla.zip");
        URI localResourceURI = URI.create("jar:file:/bla/1234!/globi.json");

        URI remoteResourceURI = CacheLocalReadonly.getRemoteJarURIIfNeeded(remoteArchiveURI, localResourceURI);
        assertThat(remoteResourceURI.toString(), is("jar:file:/bla/1234!/globi.json"));
    }

}