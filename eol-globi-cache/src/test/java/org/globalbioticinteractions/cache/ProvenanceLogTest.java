package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ProvenanceLogTest {

    private File tempDirectory;

    @Before
    public void init() throws IOException {
        tempDirectory = new File("target/provenance-test" + UUID.randomUUID());
        FileUtils.forceMkdir(tempDirectory);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDirectory);
    }


    @Test
    public void accessLogEntry() {
        ContentProvenance meta = new ContentProvenance("some/namespace", URI.create("http://example.com"), URI.create("cached:file.zip"), "1234", "1970-01-01T00:00:00Z");
        List<String> strings = ProvenanceLog.compileLogEntries(meta);
        assertThat(strings, is(Arrays.asList("some/namespace", "http://example.com", "1234", "1970-01-01T00:00:00Z", null)));
    }

    @Test
    public void appendToProvenanceLog() throws IOException {
        CacheLocalReadonly cache = new CacheLocalReadonly("some/namespace", tempDirectory.getAbsolutePath());
        assertNull(cache.provenanceOf(URI.create("http://example.com")));

        ContentProvenance meta = new ContentProvenance("some/namespace",
                URI.create("http://example.com"),
                URI.create("cached:file.zip"), "1234",
                "1970-01-01T00:00:00Z");
        ProvenanceLog.appendProvenanceLog(tempDirectory, meta);

        ContentProvenance contentProvenance = cache.provenanceOf(URI.create("http://example.com"));
        assertThat(contentProvenance.getNamespace(), is("some/namespace"));
        assertThat(contentProvenance.getSourceURI().toString(), is("http://example.com"));
        assertThat(contentProvenance.getSha256(), is("1234"));
        assertThat(contentProvenance.getAccessedAt(), is("1970-01-01T00:00:00Z"));
        assertThat(contentProvenance.getType(), is(nullValue()));
    }

    @Test
    public void detectJarEntry() {
        ContentProvenance meta = new ContentProvenance(
                "some/namespace",
                URI.create("http://example.com"),
                URI.create("jar:file://file.zip!/something"),
                "1234",
                "1970-01-01");
        assertFalse(ProvenanceLog.needsCaching(meta, tempDirectory));
    }

    @Test
    public void remoteURINeedsCaching() throws IOException, URISyntaxException {
        ContentProvenance meta = new ContentProvenance(
                "some/namespace",
                URI.create("http://example.com"),
                new File(tempDirectory, "somefile.txt").toURI(),
                "1234",
                "1970-01-01");
        assertTrue(ProvenanceLog.needsCaching(meta, tempDirectory));
    }

    @Test
    public void localURIOutsideOfCacheDirNeedsCaching() throws IOException, URISyntaxException {
        ContentProvenance meta = new ContentProvenance(
                "some/namespace",
                tempDirectory.getParentFile().toURI(),
                new File(tempDirectory, "somefile.txt").toURI(),
                "1234",
                "1970-01-01");
        assertTrue(ProvenanceLog.needsCaching(meta, tempDirectory));
    }

    @Test
    public void localURIInsideOfCacheDirNeedsCaching() throws IOException, URISyntaxException {
        ContentProvenance meta = new ContentProvenance(
                "some/namespace",
                new File(tempDirectory, "somefile.txt").toURI(),
                new File(tempDirectory, "somefile.txt").toURI(),
                "1234",
                "1970-01-01");
        assertFalse(ProvenanceLog.needsCaching(meta, tempDirectory));
    }


}