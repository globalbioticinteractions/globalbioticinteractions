package org.eol.globi.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ProxyInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class ResourceUtilTest {

    @Test
    public void URIString() {
        URI bobo = URI.create("bobo");
        assertThat(bobo.toString(), is("bobo"));
    }

    @Test
    public void localJarResource() throws IOException {
        URL resource = getClass().getResource("/java/lang/String.class");
        InputStream inputStream = ResourceUtil.asInputStream(resource.toString());
        assertNotNull(inputStream);
        inputStream.close();
        assertThat(inputStream.available(), is(0));

    }

    @Test
    public void localJarResourceWithInputStreamFactory() throws URISyntaxException, IOException {
        URL resource = getClass().getResource("/java/lang/String.class");
        AtomicLong counter = new AtomicLong(0);
        InputStream inputStream = ResourceUtil.asInputStream(resource.toString(), new InputStreamFactory() {
            @Override
            public InputStream create(InputStream inStream) throws IOException {
                return new ProxyInputStream(inStream) {
                    @Override
                    protected void afterRead(int n) throws IOException {
                        counter.addAndGet(n);
                    }
                };
            }
        });
        assertNotNull(inputStream);
        IOUtils.copy(inputStream, new NullOutputStream());
        inputStream.close();

        assertThat(counter.get(), greaterThan(100L));

    }

    @Test
    public void kaboomWithInputStreamFactory() throws URISyntaxException {
        URL resource = getClass().getResource("/java/lang/String.class");
        try {
            ResourceUtil.asInputStream(resource.toString(), new InputStreamFactory() {
                @Override
                public InputStream create(InputStream inStream) throws IOException {
                    throw new IOException("kaboom!");
                }
            });
            fail("expected IOException");
        } catch (IOException ex) {
            assertThat(ex.getCause().getMessage(), is("kaboom!"));
        }

    }

    @Test
    public void relativeURI() {
        URI uri = ResourceUtil.getAbsoluteResourceURI(URI.create("some:/example/"), URI.create("/path"));
        assertThat(uri.toString(), is("some:/example/path"));
    }

    @Test
    public void relativeURINoSlash() {
        URI uri = ResourceUtil.getAbsoluteResourceURI(URI.create("some:/example"), URI.create("path"));
        assertThat(uri.toString(), is("some:/example/path"));
    }

    @Test
    public void relativeURISlashContext() {
        URI uri = ResourceUtil.getAbsoluteResourceURI(URI.create("some:/example/"), URI.create("path"));
        assertThat(uri.toString(), is("some:/example/path"));
    }

    @Test
    public void relativeURISlashResource() throws URISyntaxException {
        URI uri = ResourceUtil.getAbsoluteResourceURI(URI.create("some:/example"), URI.create("/path"));
        assertThat(uri.toString(), is("some:/example/path"));
    }


}