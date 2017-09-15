package org.eol.globi.util;

import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class BlobStoreTmpCacheTest {

    @Test
    public void localResource() throws URISyntaxException {
        URL resource = getClass().getResource(getClass().getSimpleName() + ".class");
        assertThat(new File(resource.toURI()).exists(), is(true));
        assertTrue(new BlobStoreTmpCache().resourceExists(resource.toURI()));
    }

    @Test
    public void relativeURI() throws URISyntaxException {
        URI uri = new BlobStoreTmpCache().getAbsoluteResourceURI(URI.create("some:/example/"), "/path");
        assertThat(uri.toString(), is("some:/example/path"));
    }

    @Test
    public void relativeURINoSlash() throws URISyntaxException {
        URI uri = new BlobStoreTmpCache().getAbsoluteResourceURI(URI.create("some:/example"), "path");
        assertThat(uri.toString(), is("some:/example/path"));
    }

   @Test
    public void relativeURISlashContext() throws URISyntaxException {
        URI uri = new BlobStoreTmpCache().getAbsoluteResourceURI(URI.create("some:/example/"), "path");
        assertThat(uri.toString(), is("some:/example/path"));
    }

   @Test
    public void relativeURISlashResource() throws URISyntaxException {
        URI uri = new BlobStoreTmpCache().getAbsoluteResourceURI(URI.create("some:/example"), "/path");
        assertThat(uri.toString(), is("some:/example/path"));
    }


}