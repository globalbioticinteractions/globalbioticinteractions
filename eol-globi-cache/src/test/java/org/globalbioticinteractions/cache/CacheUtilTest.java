package org.globalbioticinteractions.cache;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

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


}