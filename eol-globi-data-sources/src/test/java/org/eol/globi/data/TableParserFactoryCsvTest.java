package org.eol.globi.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class TableParserFactoryCsvTest extends TableParserFactoryCsvTestBase {

    @Override
    public TableParserFactoryCsv getTableParserFactory() {
        return new TableParserFactoryCsv();
    }

    @Test
    public void encoding_iso_8859_1() throws IOException {
        // reproduces root cause of https://github.com/globalbioticinteractions/globalbioticinteractions/issues/1200
        TableParserFactory tableParserFactory = getTableParserFactory();
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("url", "foo.csv");
        objectNode.put("delimiter", ",");
        objectNode.put("encoding", "iso-8859-1");
        CSVParse parser = tableParserFactory.createParser(objectNode, new DatasetImpl("foo/bar", new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return getClass().getResourceAsStream("virion-subset-iso8859-1.csv");
            }
        }, URI.create("https://example.org/archive.zip")));

        String[] line = parser.getLine();
        line = parser.getLine();
        assertThat(line, Is.is(notNullValue()));

        assertThat(line[0], Is.is ("Homo sapiens"));
        assertThat(line[7], Is.is ("Miró G, 2007, Vet Parasitol, 143"));
    }

    @Test
    public void quotedTable() throws IOException {
        // reproduces root cause of https://github.com/globalbioticinteractions/globalbioticinteractions/issues/1200
        TableParserFactory tableParserFactory = getTableParserFactory();
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("url", "foo.csv");
        objectNode.put("delimiter", ",");
        CSVParse parser = tableParserFactory.createParser(objectNode, new DatasetImpl("foo/bar", new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return IOUtils.toInputStream("one,two\na value,\"host\":\"Menegazzia albida\"");
            }
        }, URI.create("https://example.org/archive.zip")));

        String[] line = parser.getLine();
        line = parser.getLine();
        assertThat(line, Is.is(notNullValue()));

        assertThat(line[0], Is.is ("a value"));
        assertThat(line[1], Is.is ("host:\"Menegazzia albida\""));
    }

}