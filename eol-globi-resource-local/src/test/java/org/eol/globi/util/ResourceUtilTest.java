package org.eol.globi.util;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResourceUtilTest {

    @Test
    public void URIString() {
        URI bobo = URI.create("bobo");
        assertThat(bobo.toString(), is("bobo"));
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