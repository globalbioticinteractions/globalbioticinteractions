package org.globalbioticinteractions.cache;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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

    @Test
    public void resourceInCachedJar() throws URISyntaxException {
        String namespaceCacheDir = "/test-cache/globalbioticinteractions/template-dataset/";
        URL resource = getClass().getResource(namespaceCacheDir + "access.tsv");
        URL archive = getClass().getResource(namespaceCacheDir + "631d3777cf83e1abea848b59a6589c470cf0c7d0fd99682c4c104481ad9a543f");
        String cacheDir = new File(resource.toURI()).getParentFile().getParentFile().getParent();
        CacheLocalReadonly cacheLocalReadonly = new CacheLocalReadonly("globalbioticinteractions/template-dataset", cacheDir);
        CachedURI cachedURI = cacheLocalReadonly.asMeta(URI.create("jar:" + archive.toString() + "!/globi.json"));
        assertThat(cachedURI.getSourceURI().toString(), is("jar:https://zenodo.org/record/207958/files/globalbioticinteractions/template-dataset-0.0.2.zip!/globi.json"));
        assertThat(cachedURI.getAccessedAt(), is("2017-09-14T16:39:33Z"));
    }

    @Test
    public void remoteResource() throws URISyntaxException {
        String namespaceCacheDir = "/test-cache/globalbioticinteractions/template-dataset/";
        URL resource = getClass().getResource(namespaceCacheDir + "access.tsv");
        URL archiveURL = getClass().getResource(namespaceCacheDir + "631d3777cf83e1abea848b59a6589c470cf0c7d0fd99682c4c104481ad9a543f");
        String cacheDir = new File(resource.toURI()).getParentFile().getParentFile().getParent();
        CacheLocalReadonly cacheLocalReadonly = new CacheLocalReadonly("globalbioticinteractions/template-dataset", cacheDir);
        CachedURI cachedURI = cacheLocalReadonly.asMeta(URI.create("https://zenodo.org/record/207958/files/globalbioticinteractions/template-dataset-0.0.2.zip"));
        assertThat(cachedURI.getSourceURI().toString(), is("https://zenodo.org/record/207958/files/globalbioticinteractions/template-dataset-0.0.2.zip"));
        assertThat(cachedURI.getAccessedAt(), is("2017-09-14T16:39:33Z"));
        assertThat(cachedURI.getCachedURI(), is(archiveURL.toURI()));
    }

}