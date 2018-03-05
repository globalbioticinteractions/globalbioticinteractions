package org.globalbioticinteractions.cache;

import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

public class CacheLogTest {

    @Test
    public void accessLogEntry() throws IOException, URISyntaxException {
        CachedURI meta = new CachedURI("some/namespace", URI.create("http://example.com"), URI.create("cached:file.zip"), "1234", "1970-01-01T00:00:00Z");
        List<String> strings = CacheLog.compileLogEntries(meta);
        assertThat(strings, is(Arrays.asList("some/namespace", "http://example.com", "1234", "1970-01-01T00:00:00Z", null)));
    }

    @Test
    public void accessLogJarEntry() throws IOException, URISyntaxException {
        CachedURI meta = new CachedURI("some/namespace", URI.create("http://example.com"), URI.create("jar:file://file.zip!/something"), "1234", "1970-01-01");
        List<String> strings = CacheLog.compileLogEntries(meta);
        assertThat(strings, is(empty()));
    }


}