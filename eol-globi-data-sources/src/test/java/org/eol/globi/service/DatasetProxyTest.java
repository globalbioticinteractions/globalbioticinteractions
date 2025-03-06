package org.eol.globi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetProxy;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
    public void getMappedResource() throws IOException, URISyntaxException {
        URL original = getClass().getResource("/org/globalbioticinteractions/content/original.txt");
        URL proxied = getClass().getResource("/org/globalbioticinteractions/content/proxied.txt");
        JsonNode configProxy = new ObjectMapper().readTree("{ \"resources\": { \"archive\": \"" + proxied.toURI() + "\" } }");
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("http://example.com"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        dataset.setConfig(null);


        final String hashOfOriginal = "0682c5f2076f099c34cfdd15a9e063849ed437a49677e6fcc5b4198c76575be5";
        final String hashOfProxied = "b09f17ea1b5ca77b7a01a3ed62c84b38578817eddb34f827666c898a27504f67";

        DatasetProxy datasetProxy = new DatasetProxy(dataset);
        datasetProxy.setConfig(configProxy);
        DatasetProxy testDataset = datasetProxy;

        TestHashUtil.assertContentHash(testDataset.retrieve(URI.create("archive")), hashOfProxied);

        JsonNode config = new ObjectMapper().readTree("{ \"resources\": { \"archive\": \"" + original.toURI() + "\" } }");
        testDataset = getTestDataset(config, configProxy);

        TestHashUtil.assertContentHash(testDataset.retrieve(URI.create("archive")), hashOfProxied);

        testDataset = getTestDataset(config, null);

        TestHashUtil.assertContentHash(testDataset.retrieve(URI.create("archive")), hashOfOriginal);

    }

    @Test
    public void getMappedResourceWithConfig() throws IOException, URISyntaxException {
        URL original = getClass().getResource("/org/globalbioticinteractions/content/original.txt");
        URL proxied = getClass().getResource("/org/globalbioticinteractions/content/proxied.txt");
        JsonNode configProxy = new ObjectMapper().readTree("{ \"resources\": { \"archive\": \"" + proxied.toURI() + "\" } }");
        JsonNode config = new ObjectMapper().readTree("{ \"resources\": { \"archive\": \"" + original.toURI() + "\", \"anotherArchive\": \"" + original.toURI() + "\" } }");
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("http://example.com"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        dataset.setConfig(config);

        DatasetProxy datasetProxy = new DatasetProxy(dataset);
        datasetProxy.setConfig(configProxy);

        assertThat(datasetProxy.getConfig().at("/resources/archive").asText(), endsWith("proxied.txt"));
        assertThat(dataset.getConfig().at("/resources/anotherArchive").asText(), endsWith("original.txt"));
        assertThat(datasetProxy.getConfig().at("/resources/anotherArchive").asText(), endsWith("original.txt"));

    }

    @Test
    public void getDoNotMergeTableSchema() throws IOException {
        JsonNode configProxy = new ObjectMapper().readTree(getClass().getResourceAsStream("/org/eol/globi/data/example-no-eol.json"));
        JsonNode config = new ObjectMapper().readTree(getClass().getResourceAsStream("/org/eol/globi/data/example-no-eol.json"));
        DatasetImpl dataset = new DatasetWithResourceMapping("some/namespace", URI.create("http://example.com"), new ResourceServiceLocal(new InputStreamFactoryNoop()));
        dataset.setConfig(config);

        DatasetProxy datasetProxy = new DatasetProxy(dataset);
        datasetProxy.setConfig(configProxy);

        assertThat(datasetProxy.getConfig().at("/tableSchema/columns").size(), is(2));
        assertThat(dataset.getConfig().at("/tableSchema/columns").size(), is(2));

    }

    @Test
    public void getMetaTableProxy() throws IOException, URISyntaxException {
        JsonNode tables = new ObjectMapper().readTree(getClass().getResourceAsStream("globi-meta.json"));
        JsonNode table = new ObjectMapper().readTree(getClass().getResourceAsStream("globi-metatable.json"));


        Dataset dataset = new DatasetImpl("foo/bar", null, URI.create("foo:bar"));
        dataset.setConfig(tables);

        DatasetProxy datasetProxy = new DatasetProxy(dataset);
        dataset.setConfig(table);

        assertThat(datasetProxy.getOrDefault("shouldResolveReferences", "false"), is("true"));
    }


    public DatasetProxy getTestDataset() {
        return getTestDataset(null, null);
    }

    public DatasetProxy getTestDataset(JsonNode config, JsonNode configProxy) {
        DatasetImpl dataset = new DatasetWithResourceMapping(
                "some/namespace",
                URI.create("http://example.com"),
                new ResourceServiceLocal(new InputStreamFactoryNoop()));

        dataset.setConfig(config);

        DatasetProxy datasetProxy = new DatasetProxy(dataset);
        datasetProxy.setConfig(configProxy);
        return datasetProxy;
    }

}