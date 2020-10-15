package org.globalbioticinteractions.dataset;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetUtilTest {

    @Test
    public void lookupMappedResourceRelative() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"), inStream -> inStream);
        dataset.setConfig(new ObjectMapper().readTree("{\"resources\": { \"previous/path.txt\": \"current/path.txt\" } }"));
        assertThat(DatasetUtil.mapResourceForDataset(dataset, URI.create("previous/path.txt")).toString(), is("some:uri/current/path.txt"));
    }

    @Test
    public void lookupMappedResourceAbsoluteToRelative() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"), inStream -> inStream);
        dataset.setConfig(new ObjectMapper().readTree("{\"resources\": { \"http://example.org/previous/path.txt\": \"current/path.txt\" } }"));
        assertThat(DatasetUtil.mapResourceForDataset(dataset, URI.create("http://example.org/previous/path.txt")).toString(), is("some:uri/current/path.txt"));
    }

    @Test
    public void lookupMappedResourceAbsoluteToAbsolute() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"), inStream -> inStream);
        dataset.setConfig(new ObjectMapper().readTree("{\"resources\": { \"http://example.org/previous/path.txt\": \"http://example.org/current/path.txt\" } }"));
        assertThat(DatasetUtil.mapResourceForDataset(dataset,URI.create("http://example.org/previous/path.txt")).toString(), is("http://example.org/current/path.txt"));
    }

    @Test
    public void lookupNonMappedResourceRelative() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"), inStream -> inStream);
        assertThat(DatasetUtil.mapResourceForDataset(dataset,URI.create("previous/path.txt")).toString(),
                is("some:uri/previous/path.txt"));
    }

    @Test
    public void lookupNonMappedResourceAbsolute() throws IOException {
        Dataset dataset = new DatasetImpl("some/namespace", URI.create("some:uri"), inStream -> inStream);
        assertThat(DatasetUtil.mapResourceForDataset(dataset,URI.create("http://example.org/previous/path.txt")).toString(),
                is("http://example.org/previous/path.txt"));
    }

}