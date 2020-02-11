package org.globalbioticinteractions.dataset;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetConstant;
import org.globalbioticinteractions.cache.Cache;
import org.globalbioticinteractions.cache.ContentProvenance;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
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
        URI cachedLocalURI = getClass().getResource("archive.zip").toURI();
        when(cache.getResourceURI(any(URI.class))).thenReturn(cachedLocalURI);
        DatasetImpl datasetUncached = new DatasetImpl("some/namespace", URI.create("some:bla"), inStream -> inStream);

        DatasetWithCache datasetWithCache = new DatasetWithCache(datasetUncached, cache);
        URI someURI = datasetWithCache.getResourceURI(URI.create("foo"));

        assertThat(someURI, is(URI.create("jar:" + cachedLocalURI.toString() + "!/template-dataset-e68f4487ebc3bc70668c0f738223b92da0598c00/foo")));
    }

    @Test
    public void getLocalDirArchiveURI() throws IOException, URISyntaxException {
        Cache cache = Mockito.mock(Cache.class);
        URI localFileURI = getClass().getResource("archive.zip").toURI();
        URI cachedLocalURI = new File(localFileURI).getParentFile().toURI();
        assertTrue(DatasetWithCache.isLocalDir(cachedLocalURI));
        when(cache.getResourceURI(any(URI.class))).thenReturn(localFileURI);
        DatasetImpl datasetUncached = new DatasetImpl("some/namespace", cachedLocalURI, inStream -> inStream);

        DatasetWithCache datasetWithCache = new DatasetWithCache(datasetUncached, cache);
        URI someURI = datasetWithCache.getResourceURI(URI.create("foo.txt"));

        assertThat(someURI, is(URI.create(cachedLocalURI.toString() + "archive.zip")));
    }

    @Test
    public void doNotCacheLocalDirResourceURI() throws IOException, URISyntaxException {
        Cache cache = Mockito.mock(Cache.class);
        URI localFileURI = getClass().getResource("archive.zip").toURI();
        URI cachedLocalURI = new File(localFileURI).getParentFile().toURI();
        assertTrue(DatasetWithCache.isLocalDir(cachedLocalURI));
        when(cache.getResourceURI(any(URI.class))).thenReturn(localFileURI);
        DatasetImpl datasetUncached = new DatasetImpl("some/namespace", cachedLocalURI, inStream -> inStream);

        DatasetWithCache datasetWithCache = new DatasetWithCache(datasetUncached, cache);
        URI someURI = datasetWithCache.getResourceURI(cachedLocalURI);

        assertThat(someURI, not(is(localFileURI)));
        assertThat(someURI, is(cachedLocalURI));
    }

    @Test
    public void getURIAbsolute() throws IOException, URISyntaxException {
        String resourceName = "https://example.org/foo";
        URI resourceURI = URI.create(resourceName);
        Cache cache = Mockito.mock(Cache.class);
        URI cachedLocalURI = URI.create("someCached.txt");
        when(cache.getResourceURI(resourceURI)).thenReturn(cachedLocalURI);
        DatasetImpl datasetUncached = new DatasetImpl("some/namespace", URI.create("some:bla"), inStream -> inStream);

        DatasetWithCache datasetWithCache = new DatasetWithCache(datasetUncached, cache);
        URI someURI = datasetWithCache.getResourceURI(URI.create(resourceName));

        assertThat(someURI, is(URI.create("someCached.txt")));
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