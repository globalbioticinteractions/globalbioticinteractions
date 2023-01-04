package org.eol.globi.export;

import org.eol.globi.data.GraphDBNeo4j2TestCase;
import org.eol.globi.data.NodeFactoryException;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;

public class ExportNamespacesTest extends GraphDBNeo4j2TestCase {

    @Test
    public void export() throws IOException, NodeFactoryException {
        StringWriter writer = new StringWriter();
        createDataset("namespace1");
        createDataset("namespace2");
        ExportUtil.export(ExportUtil.AppenderWriter.of(writer), getGraphDb(), ExportNamespaces.CYPHER_QUERY);

        assertThat(writer.toString(), Is.is("namespace\nnamespace1\nnamespace2\n"
        ));
    }

    private void createDataset(String namespace2) throws NodeFactoryException {
        getNodeFactory().getOrCreateDataset(new DatasetImpl(namespace2, resourceName -> {
            throw new IOException("boom!");
        }, URI.create("foo:bar")));
    }

}