package org.eol.globi.data;

import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.ResourceService;
import org.eol.globi.taxon.ResolvingTaxonIndex;
import org.eol.globi.tool.NodeFactoryFactory;
import org.eol.globi.tool.NodeFactoryFactoryTransactingOnDataset;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.NodeIdCollectorNeo4j3;
import org.eol.globi.util.ResourceServiceHTTP;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

public class GraphDBNeo4jTestCase extends GraphDBTestCaseAbstract {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();


    private File cacheDir = null;

    public File getCacheDir() {
        if (cacheDir == null) {
            try {
                cacheDir = folder.newFolder();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return cacheDir;
    }

    protected TaxonIndex createTaxonIndex(PropertyEnricher enricher) {
        return new ResolvingTaxonIndex(enricher, getGraphDb());
    }

    public GraphDatabaseService getGraphDb() {
        return getNodeFactory().getGraphDb();
    }

    protected NodeIdCollectorNeo4j3 getNodeIdCollector() {
        return new NodeIdCollectorNeo4j3();
    }

    @Override
    protected NodeFactoryNeo4j createNodeFactory() {
        NodeFactoryFactory factoryFactory;

        final GraphDatabaseService graphDatabaseService = neo4j.defaultDatabaseService();
        factoryFactory
                = new NodeFactoryFactoryTransactingOnDataset(new GraphServiceFactory() {
            @Override
            public GraphDatabaseService getGraphService() {
                return graphDatabaseService;
            }

            @Override
            public void close() throws Exception {

            }
        });

        try (Transaction tx = graphDatabaseService.beginTx()) {
            NodeFactory nodeFactoryNeo4j = factoryFactory.create(graphDatabaseService, cacheDir);
            assertThat(nodeFactoryNeo4j, Is.is(instanceOf(NodeFactoryNeo4j.class)));
            NodeFactoryNeo4j factory = (NodeFactoryNeo4j) nodeFactoryNeo4j;
            factory.setEnvoLookupService(getEnvoLookupService());
            factory.setTermLookupService(getTermLookupService());
            tx.commit();
            return factory;
        }
    }

    protected ResourceService getResourceService() {
        return new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop(), getCacheDir());
    }

    protected ResourceServiceHTTP getResourceServiceHTTP() {
        return new ResourceServiceHTTP(new InputStreamFactoryNoop(), getCacheDir());
    }


}
