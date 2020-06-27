package org.globalbioticinteractions.dataset;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.globalbioticinteractions.cache.Cache;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.ContentProvenance;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class DatasetWithCacheTest {

    @Test
    public void citationWithCitationWithLastAccessed() throws IOException {
        Cache cache = Mockito.mock(Cache.class);
        ContentProvenance cacheURI = Mockito.mock(ContentProvenance.class);
        when(cacheURI.getAccessedAt()).thenReturn("1970-01-01");
        when(cache.provenanceOf(any(URI.class))).thenReturn(cacheURI);
        Dataset datasetUncached = Mockito.mock(Dataset.class);
        when(datasetUncached.getNamespace()).thenReturn("some/namespace");
        when(datasetUncached.getOrDefault("citation", "")).thenReturn("some citation");
        when(datasetUncached.getOrDefault(PropertyAndValueDictionary.DCTERMS_BIBLIOGRAPHIC_CITATION,
                "some citation"))
                .thenReturn("some citation");

        DatasetWithCache dataset = new DatasetWithCache(datasetUncached, cache);
        assertThat(dataset.getCitation(), Is.is("some citation"));
    }

    @Test
    public void citationWithLastAccessed() {
        DatasetWithCache dataset = datasetLastAccessedAt("1970-01-01");
        assertThat(dataset.getCitation(), Is.is("Accessed on 1970-01-01 via <some:bla>."));
    }

    @Test
    public void citationWithoutLastAccessed() {
        DatasetWithCache dataset = datasetLastAccessedAt(null);
        assertThat(dataset.getCitation(), Is.is("Accessed via <some:bla>."));
    }

    @Test
    public void getURIRelative() throws IOException, URISyntaxException {
        Cache cache = Mockito.mock(Cache.class);
        when(cache.retrieve(URI.create("some:bla/foo"))).thenReturn(IOUtils.toInputStream("relative"));
        DatasetImpl datasetUncached = new DatasetImpl("some/namespace", URI.create("some:bla"), inStream -> inStream);

        DatasetWithCache datasetWithCache = new DatasetWithCache(datasetUncached, cache);
        InputStream is = datasetWithCache.retrieve(URI.create("foo"));
        assertThat(IOUtils.toString(is, StandardCharsets.UTF_8), is("relative"));
    }

    @Test
    public void getLocalDirArchiveURI() throws IOException, URISyntaxException {
        Cache cache = Mockito.mock(Cache.class);
        URI localFileURI = getClass().getResource("archive.zip").toURI();
        URI cachedLocalURI = new File(localFileURI).getParentFile().toURI();
        assertTrue(CacheUtil.isLocalDir(cachedLocalURI));
        when(cache.retrieve(URI.create(cachedLocalURI.toString() + "foo.txt"))).thenReturn(IOUtils.toInputStream("relative", StandardCharsets.UTF_8));
        DatasetImpl datasetUncached = new DatasetImpl("some/namespace", cachedLocalURI, inStream -> inStream);

        DatasetWithCache datasetWithCache = new DatasetWithCache(datasetUncached, cache);
        InputStream is = datasetWithCache.retrieve(URI.create("foo.txt"));
        assertThat(IOUtils.toString(is, StandardCharsets.UTF_8), is("relative"));
    }

    @Test(expected = IOException.class)
    public void doNotCacheLocalDirResourceURI() throws IOException, URISyntaxException {
        Cache cache = Mockito.mock(Cache.class);
        URI localFileURI = getClass().getResource("archive.zip").toURI();
        URI cachedLocalURI = new File(localFileURI).getParentFile().toURI();
        assertTrue(CacheUtil.isLocalDir(cachedLocalURI));
        when(cache.retrieve(any(URI.class))).thenThrow(new IOException("kaboom!"));

        DatasetImpl datasetUncached = new DatasetImpl("some/namespace", cachedLocalURI, inStream -> inStream);

        DatasetWithCache datasetWithCache = new DatasetWithCache(datasetUncached, cache);
        try {
            datasetWithCache.retrieve(cachedLocalURI);
        } catch (IOException ex) {
            assertThat(ex.getMessage(), is("kaboom!"));
            throw ex;
        }

    }

    @Test
    public void getURIAbsolute() throws IOException, URISyntaxException {
        String resourceName = "https://example.org/foo";
        URI resourceURI = URI.create(resourceName);
        Cache cache = Mockito.mock(Cache.class);
        when(cache.retrieve(resourceURI)).thenReturn(IOUtils.toInputStream("cached", StandardCharsets.UTF_8));
        DatasetImpl datasetUncached = new DatasetImpl("some/namespace", URI.create("some:bla"), inStream -> inStream);

        DatasetWithCache datasetWithCache = new DatasetWithCache(datasetUncached, cache);
        InputStream is = datasetWithCache.retrieve(URI.create(resourceName));

        assertThat(IOUtils.toString(is, StandardCharsets.UTF_8), is("cached"));
    }

    private DatasetWithCache datasetLastAccessedAt(String lastAccessed) {
        Cache cache = Mockito.mock(Cache.class);
        ContentProvenance cacheURI = Mockito.mock(ContentProvenance.class);
        when(cacheURI.getAccessedAt()).thenReturn(lastAccessed);
        when(cache.provenanceOf(any(URI.class))).thenReturn(cacheURI);
        Dataset datasetUncached = new DatasetImpl("some/namespace", URI.create("some:bla"), inStream -> inStream);
        return new DatasetWithCache(datasetUncached, cache);
    }

    @Test
    public void requestLastSeenAtTwice() throws IOException {
        assertInvokedOnce(DatasetConstant.LAST_SEEN_AT, createCacheMockLastSeen());
    }

    @Test
    public void requestContentHashTwice() throws IOException {
        assertInvokedOnce(DatasetConstant.CONTENT_HASH, createCacheMockContentHash());
    }

    private void assertInvokedOnce(String propertyName, Cache cacheMock) {
        DatasetImpl datasetUncached = new DatasetImpl("some/namespace", URI.create("some:bla"), inStream -> inStream);

        DatasetWithCache datasetWithCache = new DatasetWithCache(datasetUncached, cacheMock);
        String firstLastSeen = datasetWithCache.getOrDefault(propertyName, "");
        assertThat(firstLastSeen, is("first"));
        String secondLastSeen = datasetWithCache.getOrDefault(propertyName, "");
        assertThat(secondLastSeen, is("first"));
    }

    private Cache createCacheMockLastSeen() {
        Cache cache = Mockito.mock(Cache.class);
        ContentProvenance firstContentProvenance = Mockito.mock(ContentProvenance.class);
        when(firstContentProvenance.getAccessedAt()).thenReturn("first");
        ContentProvenance secondContentProvenance = Mockito.mock(ContentProvenance.class);
        when(secondContentProvenance.getAccessedAt()).thenReturn("second");
        when(cache.provenanceOf(URI.create("some:bla")))
                .thenReturn(firstContentProvenance)
                .thenReturn(secondContentProvenance);
        return cache;
    }

    private Cache createCacheMockContentHash() {
        Cache cache = Mockito.mock(Cache.class);
        ContentProvenance firstContentProvenance = Mockito.mock(ContentProvenance.class);
        when(firstContentProvenance.getSha256()).thenReturn("first");
        ContentProvenance secondContentProvenance = Mockito.mock(ContentProvenance.class);
        when(secondContentProvenance.getSha256()).thenReturn("second");
        when(cache.provenanceOf(URI.create("some:bla")))
                .thenReturn(firstContentProvenance)
                .thenReturn(secondContentProvenance);
        return cache;
    }

}