package org.globalbioticinteractions.cache;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class CacheUtilTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void inCacheDir() throws IOException {
        File cacheDir = folder.newFolder();
        assertTrue(CacheUtil.isInCacheDir(cacheDir, new File(cacheDir, "foo.txt").toURI()));
    }


    @Test
    public void inCacheDirAndJar() throws IOException {
        File cacheDir = folder.newFolder();
        assertTrue(CacheUtil.isInCacheDir(cacheDir, URI.create("jar:" + new File(cacheDir, "archive.zip").toURI().toString()+ "!/foo.txt")));
    }

    @Test
    public void notInCacheDir() throws IOException {
        File cacheDir = folder.newFolder("cacheDir");
        File notCacheDir = folder.newFolder("notCacheDir");
        assertFalse(CacheUtil.isInCacheDir(cacheDir, new File(notCacheDir, "foo.txt").toURI()));
    }


    @Test
    public void notInCacheDirAtAll() throws IOException {
        File cacheDir = folder.newFolder("cacheDir");
        assertFalse(CacheUtil.isInCacheDir(cacheDir, URI.create("https://example.org")));
    }

    @Test
    public void notInCacheDirNotJar() throws IOException {
        File cacheDir = folder.newFolder();
        File notCacheDir = folder.newFolder("notCacheDir");
        assertFalse(CacheUtil.isInCacheDir(cacheDir, URI.create("jar:" + new File(notCacheDir, "archive.zip").toURI().toString()+ "!/foo.txt")));
    }

    @Test
    public void cacheForDirNamespace() throws IOException {
        File cachePath = folder.newFolder();
        String namespace = "urn:lsid:checklistbank.org:dataset:2017";
        File cacheDirForNamespace = CacheUtil.findCacheDirForNamespace(cachePath, namespace);
        assertThat(
                StringUtils.removeStart(cacheDirForNamespace.getPath(), cachePath.getPath()),
                Is.is("/urn/lsid/checklistbank.org/dataset/2017"));
    }

    @Test
    public void cacheForDirImplicitGloBINamespace() throws IOException {
        File cachePath = folder.newFolder();
        String namespace = "some/namespace";
        File cacheDirForNamespace = CacheUtil.findCacheDirForNamespace(cachePath, namespace);
        assertThat(
                StringUtils.removeStart(cacheDirForNamespace.getPath(), cachePath.getPath()),
                Is.is("/" + namespace)
        );
    }

}