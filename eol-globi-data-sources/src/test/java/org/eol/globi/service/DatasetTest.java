package org.eol.globi.service;

import org.junit.Test;

import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DatasetTest {

    @Test
    public void resourceURL() {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"));
        assertThat(URI.create("http://otherexample.com/bla"), is(dataset.getResourceURI("http://otherexample.com/bla")));
        assertThat(URI.create("http://example.com/someResource"), is(dataset.getResourceURI("/someResource")));
        assertThat(URI.create("http://example.com/someResource"), is(dataset.getResourceURI("someResource")));
    }


}