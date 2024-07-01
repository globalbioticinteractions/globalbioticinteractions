package org.globalbioticinteractions.cache;

import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
public class CachePullThroughTest {

    @Test
    public void cache() throws IOException {
        File cacheDir = CacheUtil.findOrMakeCacheDirForNamespace("target/cache/datasets", "some/namespace");
        ContentProvenance contentProvenance = CachePullThrough.cache(URI.create("https://github.com/globalbioticinteractions/template-dataset/archive/main.zip"), cacheDir, new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop()));
        File cachedFile = new File(contentProvenance.getLocalURI());
        assertThat(cachedFile.exists(), CoreMatchers.is(true));
        assertThat(cachedFile.toURI().toString(), startsWith("file:/"));
    }


}