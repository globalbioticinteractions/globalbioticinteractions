package org.globalbioticinteractions.cache;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;

public class CacheProxyTest {

    @Test
    public void oneThrowsOtherSucceeds() throws IOException {
        Cache cache = new CacheProxy(
                Arrays.asList(new MyExplodingCache(), new MyFooCache())
        );

        InputStream retrieve = cache.retrieve(URI.create("https://example.org/foo.txt"));

        assertThat(IOUtils.toString(retrieve, StandardCharsets.UTF_8), Is.is("foo"));
    }

    @Test(expected = IOException.class)
    public void twoThrow() throws IOException {
        Cache cache = new CacheProxy(
                Arrays.asList(new MyExplodingCache(), new MyExplodingCache())
        );

        cache.retrieve(URI.create("https://example.org/foo.txt"));
    }

    @Test
    public void oneSucceedsAnotherThrows() throws IOException {
        Cache cache = new CacheProxy(
                Arrays.asList(new MyFooCache(), new MyExplodingCache())
        );

        InputStream retrieve = cache.retrieve(URI.create("https://example.org/foo.txt"));

        assertThat(IOUtils.toString(retrieve, StandardCharsets.UTF_8), Is.is("foo"));
    }

    private static class MyExplodingCache implements Cache {

        @Override
        public InputStream retrieve(URI resourceName) throws IOException {
            throw new IOException("kaboom!");
        }

        @Override
        public ContentProvenance provenanceOf(URI resourceURI) {
            return null;
        }
    }

    private static class MyFooCache implements Cache {

        @Override
        public InputStream retrieve(URI resourceName) throws IOException {
            return IOUtils.toInputStream("foo", StandardCharsets.UTF_8);
        }

        @Override
        public ContentProvenance provenanceOf(URI resourceURI) {
            return null;
        }
    }
}