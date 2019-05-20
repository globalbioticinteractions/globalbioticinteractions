package org.eol.globi.util;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ResourceUtilTest {

    @Test
    public void localResource() throws URISyntaxException {
        URL resource = getClass().getResource(getClass().getSimpleName() + ".class");
        assertThat(new File(resource.toURI()).exists(), is(true));
        assertTrue(ResourceUtil.resourceExists(resource.toURI()));
    }

    @Test
    public void localJarResource() throws URISyntaxException, IOException {
        URL resource = getClass().getResource("/java/lang/String.class");
        assertTrue(ResourceUtil.resourceExists(resource.toURI()));
        InputStream inputStream = ResourceUtil.asInputStream(resource.toString());
        assertNotNull(inputStream);
        inputStream.close();
        assertThat(inputStream.available(), is(0));

    }

    @Test
    public void localResourceNull() throws URISyntaxException {
        URL resource = getClass().getResource(getClass().getSimpleName() + ".class");
        assertThat(new File(resource.toURI()).exists(), is(true));
        assertFalse(ResourceUtil.resourceExists(null));
    }

    @Test
    public void relativeURI() throws URISyntaxException {
        URI uri = ResourceUtil.getAbsoluteResourceURI(URI.create("some:/example/"), "/path");
        assertThat(uri.toString(), is("some:/example/path"));
    }

    @Test
    public void relativeURINoSlash() throws URISyntaxException {
        URI uri = ResourceUtil.getAbsoluteResourceURI(URI.create("some:/example"), "path");
        assertThat(uri.toString(), is("some:/example/path"));
    }

   @Test
    public void relativeURISlashContext() throws URISyntaxException {
        URI uri = ResourceUtil.getAbsoluteResourceURI(URI.create("some:/example/"), "path");
        assertThat(uri.toString(), is("some:/example/path"));
    }

   @Test
    public void relativeURISlashResource() throws URISyntaxException {
        URI uri = ResourceUtil.getAbsoluteResourceURI(URI.create("some:/example"), "/path");
        assertThat(uri.toString(), is("some:/example/path"));
    }


}