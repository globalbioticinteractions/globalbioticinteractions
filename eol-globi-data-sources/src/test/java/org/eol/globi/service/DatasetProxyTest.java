package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

public class DatasetProxyTest {

    @Test
    public void getOrDefault() {
        assertThat(getTestDataset().getOrDefault("foo", null), Is.is(nullValue()));
    }

    @Test
    public void getCitation() throws IOException {
        assertCitationProxies("citation");
    }

    @Test
    public void getDOI() throws IOException {
        assertThat(getTestDataset().getCitation(), Is.is("<http://example.com>"));

        JsonNode configProxy = new ObjectMapper().readTree("{ \"" + "doi" + "\": \"10.12/235\" }");
        assertThat(getTestDataset(null, configProxy).getDOI().toString(), Is.is("10.12/235"));

        JsonNode config = new ObjectMapper().readTree("{ \"" + "doi" + "\": \"10.23/456\" }");
        assertThat(getTestDataset(config, configProxy).getDOI().toString(), Is.is("10.12/235"));

        assertThat(getTestDataset(config, null).getDOI().toString(), Is.is("10.23/456"));
    }

    @Test
    public void getBibliographicCitation() throws IOException {
        assertCitationProxies(PropertyAndValueDictionary.DCTERMS_BIBLIOGRAPHIC_CITATION);
    }

    public void assertCitationProxies(String citationName) throws IOException {
        assertThat(getTestDataset().getCitation(), Is.is("<http://example.com>"));

        JsonNode configProxy = new ObjectMapper().readTree("{ \"" + citationName + "\": \"some citation\" }");
        assertThat(getTestDataset(null, configProxy).getCitation(), Is.is("some citation"));

        JsonNode config = new ObjectMapper().readTree("{ \"" + citationName + "\": \"some proxied citation.\" }");
        assertThat(getTestDataset(config, configProxy).getCitation(), Is.is("some citation"));

        assertThat(getTestDataset(config, null).getCitation(), Is.is("some proxied citation."));
    }

    @Test
    public void getResource() throws IOException {
        JsonNode configProxy = new ObjectMapper().readTree("{ \"resources\": { \"archive\": \"archive.zip\" } }");
        DatasetProxy testDataset = getTestDataset(null, configProxy);
        assertThat(testDataset.getResourceURI(URI.create("someResource.csv")).toString(), Is.is("http://example.com/someResource.csv"));
        URI expectedZipURI = URI.create("http://example.com/archive.zip");
        assertThat(testDataset.getResourceURI(URI.create("archive")), Is.is(expectedZipURI));

        JsonNode config = new ObjectMapper().readTree("{ \"resources\": { \"archive\": \"someOtherArchive.zip\" } }");
        testDataset = getTestDataset(config, configProxy);
        assertThat(testDataset.getResourceURI(URI.create("archive")), Is.is(expectedZipURI));

        testDataset = getTestDataset(config, null);
        assertThat(testDataset.getResourceURI(URI.create("archive")), Is.is(URI.create("http://example.com/someOtherArchive.zip")));
    }

    @Test
    public void getMappedResource() throws IOException {
        JsonNode configProxy = new ObjectMapper().readTree("{ \"resources\": { \"archive\": \"archive.zip\" } }");
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"), inStream -> inStream);
        dataset.setConfig(null);

        DatasetProxy datasetProxy = new DatasetProxy(dataset);
        datasetProxy.setConfig(configProxy);
        DatasetProxy testDataset = datasetProxy;

        assertThat(testDataset.getResourceURI(URI.create("someResource.csv")).toString(), Is.is("http://example.com/someResource.csv"));
        assertThat(testDataset.getResourceURI(URI.create("archive")), Is.is(URI.create("http://example.com/archive.zip")));

        JsonNode config = new ObjectMapper().readTree("{ \"resources\": { \"archive\": \"someOtherArchive.zip\" } }");
        testDataset = getTestDataset(config, configProxy);
        assertThat(testDataset.getResourceURI(URI.create("archive")), Is.is(URI.create("http://example.com/archive.zip")));

        testDataset = getTestDataset(config, null);
        assertThat(testDataset.getResourceURI(URI.create("archive")), Is.is(URI.create("http://example.com/someOtherArchive.zip")));
    }

    @Test
    public void getLocalJarResource() throws IOException {
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("jar:file:/home/homer/dataset.zip!"), inStream -> inStream);
        DatasetProxy datasetProxy = new DatasetProxy(dataset);
        assertThat(datasetProxy.getResourceURI(URI.create("somefile.json")), is(URI.create("jar:file:/home/homer/dataset.zip!/somefile.json")));
        assertThat(datasetProxy.getResourceURI(URI.create("http://example.com/somefile.json")), is(URI.create("http://example.com/somefile.json")));
    }

    public DatasetProxy getTestDataset() {
        return getTestDataset(null, null);
    }

    public DatasetProxy getTestDataset(JsonNode config, JsonNode configProxy) {
        DatasetImpl dataset = new DatasetImpl("some/namespace", URI.create("http://example.com"), inStream -> inStream);
        dataset.setConfig(config);

        DatasetProxy datasetProxy = new DatasetProxy(dataset);
        datasetProxy.setConfig(configProxy);
        return datasetProxy;
    }

}