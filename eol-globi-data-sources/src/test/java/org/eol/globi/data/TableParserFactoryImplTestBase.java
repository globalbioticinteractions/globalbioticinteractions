package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public abstract class TableParserFactoryImplTestBase {


    @Test
    public void quotedTable() throws IOException {
        // reproduces root cause of https://github.com/globalbioticinteractions/globalbioticinteractions/issues/1200
        TableParserFactory tableParserFactory = getTableParserFactory();
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("url", "foo.tsv");
        objectNode.put("delimiter", "\t");
        CSVParse parser = tableParserFactory.createParser(objectNode, new DatasetImpl("foo/bar", new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return IOUtils.toInputStream("one\ttwo\na value\t\"host\":\"Menegazzia albida\"");
            }
        }, URI.create("https://example.org/archive.zip")));

        String[] line = parser.getLine();
        line = parser.getLine();
        assertThat(line, Is.is(notNullValue()));

        assertThat(line[0], Is.is ("a value"));
        assertThat(line[1], Is.is ("\"host\":\"Menegazzia albida\""));
    }

    abstract public TableParserFactory getTableParserFactory();


}