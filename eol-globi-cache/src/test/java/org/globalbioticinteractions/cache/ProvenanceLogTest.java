package org.globalbioticinteractions.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

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
        File dataDir = this.tempDirectory;
        CacheLocalReadonly cache = appendProvenance(dataDir);

        String cacheDir = dataDir.getAbsolutePath() + "/some/namespace";

        FileUtils.forceMkdir(new File(cacheDir));

        FileUtils.writeStringToFile(new File(cacheDir, "1234"), "foo", StandardCharsets.UTF_8);

        try (InputStream retrieve = cache.retrieve(URI.create("http://example.com"))) {
            assertNotNull(retrieve);
            assertThat(IOUtils.toString(retrieve, StandardCharsets.UTF_8), is("foo"));
        }
    }

    @Test
    public void appendZipToProvenanceLogRetrieveZipEntry() throws IOException {
        File dataDir = this.tempDirectory;
        CacheLocalReadonly cache = appendProvenance(dataDir);

        String cacheDir = dataDir.getAbsolutePath() + "/some/namespace";

        FileUtils.forceMkdir(new File(cacheDir));


        File file = new File(cacheDir, "1234");

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
            zipOutputStream.putNextEntry(new ZipEntry("foo.txt"));
            IOUtils.copy(new ByteArrayInputStream("bar".getBytes(StandardCharsets.UTF_8)), zipOutputStream);
            zipOutputStream.closeEntry();
        }

        assertAvailable(cache, "jar:" + file.toURI() + "!/foo.txt");
    }

    @Test(expected = FileNotFoundException.class)
    public void appendZipEntryToProvenanceLogRetrieveZipEntry() throws IOException {
        File dataDir = this.tempDirectory;
        String cacheDir = dataDir.getAbsolutePath() + "/some/namespace";
        FileUtils.forceMkdir(new File(cacheDir));
        File file = new File(cacheDir, "1234");

        CacheLocalReadonly cache = populateCacheWithZipfile(dataDir, file);

        cache.retrieve(URI.create("jar:http://example.com!/foo.txt"));
    }

    private CacheLocalReadonly populateCacheWithZipfile(File dataDir, File file) throws IOException {
        CacheLocalReadonly cache1 = new CacheLocalReadonly(
                "some/namespace",
                dataDir.getAbsolutePath(),
                this.tempDirectory.getAbsolutePath(),
                new ResourceServiceLocal(new InputStreamFactoryNoop()),
                new ContentPathFactoryDepth0(),
                new ProvenancePathFactoryImpl()
        );

        assertNull(cache1.provenanceOf(URI.create("http://example.com")));

        ContentProvenance archive = new ContentProvenance(
                "some/namespace",
                URI.create("http://example.com"),
                URI.create("cached:file.zip"),
                "1234",
                "1970-01-01T00:00:00Z"
        );

        ProvenanceLog.appendProvenanceLog(this.tempDirectory, archive);

        ContentProvenance entry = new ContentProvenance(
                "some/namespace",
                URI.create("jar:http://example.com!/foo.txt"),
                URI.create("cached:foo.txt"),
                "5678",
                "1970-01-01T00:00:00Z"
        );

        ProvenanceLog.appendProvenanceLog(this.tempDirectory, entry);

        ContentProvenance contentProvenance = cache1.provenanceOf(URI.create("jar:http://example.com!/foo.txt"));
        assertThat(contentProvenance.getNamespace(), is("some/namespace"));
        assertThat(contentProvenance.getSourceURI().toString(), is("jar:http://example.com!/foo.txt"));
        assertThat(contentProvenance.getSha256(), is("5678"));
        assertThat(contentProvenance.getAccessedAt(), is("1970-01-01T00:00:00Z"));
        assertThat(contentProvenance.getType(), is(nullValue()));


        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
            zipOutputStream.putNextEntry(new ZipEntry("foo.txt"));
            IOUtils.copy(new ByteArrayInputStream("bar".getBytes(StandardCharsets.UTF_8)), zipOutputStream);
            zipOutputStream.closeEntry();
        }
        return cache1;
    }

    @Test
    public void appendZipEntryToProvenanceLogRetrieveZipEntryLocalPath() throws IOException {
        File dataDir = this.tempDirectory;
        String cacheDir = dataDir.getAbsolutePath() + "/some/namespace";
        FileUtils.forceMkdir(new File(cacheDir));
        File file = new File(cacheDir, "1234");

        CacheLocalReadonly cache1 = populateCacheWithZipfile(dataDir, file);

        assertAvailable(cache1, "jar:" + file.toURI().toString() + "!/foo.txt");
    }

    private CacheLocalReadonly appendProvenance(File dataDir) throws IOException {
        CacheLocalReadonly cache = new CacheLocalReadonly(
                "some/namespace",
                dataDir.getAbsolutePath(),
                this.tempDirectory.getAbsolutePath(),
                new ResourceServiceLocal(new InputStreamFactoryNoop()),
                new ContentPathFactoryDepth0(),
                new ProvenancePathFactoryImpl()
        );

        assertNull(cache.provenanceOf(URI.create("http://example.com")));

        ContentProvenance meta = new ContentProvenance(
                "some/namespace",
                URI.create("http://example.com"),
                URI.create("cached:file.zip"),
                "1234",
                "1970-01-01T00:00:00Z"
        );

        ProvenanceLog.appendProvenanceLog(this.tempDirectory, meta);

        ContentProvenance contentProvenance = cache.provenanceOf(URI.create("http://example.com"));
        assertThat(contentProvenance.getNamespace(), is("some/namespace"));
        assertThat(contentProvenance.getSourceURI().toString(), is("http://example.com"));
        assertThat(contentProvenance.getSha256(), is("1234"));
        assertThat(contentProvenance.getAccessedAt(), is("1970-01-01T00:00:00Z"));
        assertThat(contentProvenance.getType(), is(nullValue()));
        return cache;
    }

    private void assertAvailable(CacheLocalReadonly cache, String filepath) throws IOException {
        try (InputStream retrieve = cache.retrieve(URI.create(filepath))) {
            assertNotNull(retrieve);
            assertThat(IOUtils.toString(retrieve, StandardCharsets.UTF_8), is("bar"));
        }
    }

    private void assertNotAvailable(CacheLocalReadonly cache, String filepath) throws IOException {
        try (InputStream retrieve = cache.retrieve(URI.create(filepath))) {
            assertNull(retrieve);
        }
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