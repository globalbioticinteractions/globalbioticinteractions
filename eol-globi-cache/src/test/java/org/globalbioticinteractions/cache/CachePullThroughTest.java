package org.globalbioticinteractions.cache;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;

public class CachePullThroughTest {

    @Test
    public void cache() throws IOException {
        File cacheDir = CacheUtil.getCacheDirForNamespace("target/cache/datasets", "some/namespace");
        File archiveCache = CachePullThrough.cache(URI.create("https://github.com/globalbioticinteractions/template-dataset/archive/master.zip"), cacheDir);
        assertThat(archiveCache.exists(), CoreMatchers.is(true));
        assertThat(archiveCache.toURI().toString(), startsWith("file:/"));
    }


}