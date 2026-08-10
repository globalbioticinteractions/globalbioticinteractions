package org.eol.globi.db;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.NodeLabel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GraphServiceFactoryImplTest {

    private Path graphdb;

    @Before
    public void createDir() throws IOException {
        graphdb = Files.createTempDirectory("graphdb");
        graphdb.toFile().deleteOnExit();
    }

    @Test
    public void startStop() throws IOException {
        try (GraphServiceFactoryImpl graphServiceFactory = new GraphServiceFactoryImpl(graphdb.toFile())) {
            GraphDatabaseService graphService = graphServiceFactory.getGraphService();
            try (Transaction tx = graphService.beginTx()) {
                Node node = tx.findNode(NodeLabel.Taxon, "hello", "world");
                assertNull(node);
                tx.commit();
            }

            try (Transaction tx = graphService.beginTx()) {
                Node node = tx.createNode(NodeLabel.Taxon);
                node.setProperty("hello", "world");
                tx.commit();
            }

            try (Transaction tx = graphService.beginTx()) {
                Node node = tx.findNode(NodeLabel.Taxon, "hello", "world");
                assertNotNull(node);
                tx.commit();
            }
        }


    }

}