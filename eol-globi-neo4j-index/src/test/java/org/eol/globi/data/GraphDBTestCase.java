package org.eol.globi.data;

import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.ResourceService;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.taxon.ResolvingTaxonIndex;
import org.eol.globi.tool.NameResolver;
import org.eol.globi.tool.NodeFactoryFactory;
import org.eol.globi.tool.NodeFactoryFactoryTransactingOnDataset;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.NodeIdCollectorNeo4j3;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.ResourceServiceHTTP;
import org.eol.globi.util.ResourceServiceLocal;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.ContentPathFactoryDepth0;
import org.globalbioticinteractions.cache.HashCalculatorSHA256;
import org.globalbioticinteractions.cache.ProvenancePathFactoryImpl;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.globalbioticinteractions.dataset.DatasetRegistryWithCache;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertNotNull;

public class GraphDBTestCase {

    protected NodeFactoryNeo4j nodeFactory;

    protected TaxonIndex taxonIndex;

    static Neo4j neo4j = null;


    @BeforeClass
    public static void initializeNeo4j() {
        GraphDBTestCase.neo4j = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withConfig(GraphDatabaseSettings.transaction_timeout, Duration.ofSeconds(1))
                .build();
    }

    @AfterClass
    public static void closeNeo4j() {
        if (neo4j != null) {
            neo4j.close();
        }
    }


    public int getSpecimenCount(StudyNode study) {
        final AtomicInteger count = new AtomicInteger(0);

        NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode())
                , relationship -> count.incrementAndGet());

        return count.get();
    }


    public static StudyNode getStudySingleton(GraphDatabaseService graphService) {
        List<StudyNode> allStudies = NodeUtil.findAllStudies(graphService);
        assertThat(allStudies.size(), is(1));
        StudyNode study = allStudies.get(0);
        assertNotNull(study);
        return study;
    }

    public static Dataset datasetFor(String namespace) throws DatasetRegistryException {
        DatasetRegistry finder = new DatasetRegistryWithCache(
                new DatasetRegistry() {
                    @Override
                    public Iterable<String> findNamespaces() throws DatasetRegistryException {
                        return Collections.emptyList();
                    }

                    @Override
                    public void findNamespaces(Consumer<String> namespaceConsumer) throws DatasetRegistryException {
                        //
                    }


                    @Override
                    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
                        return new DatasetWithResourceMapping(namespace, URI.create("some:uri"), new ResourceServiceLocal(in -> in));
                    }
                },
                dataset -> {
                    String cacheDir = "target/datasets";
                    return CacheUtil.cacheFor(
                            dataset.getNamespace(),
                            cacheDir,
                            cacheDir,
                            new ResourceServiceLocal(new InputStreamFactoryNoop()),
                            new ResourceServiceLocal(new InputStreamFactoryNoop()),
                            new ContentPathFactoryDepth0(),
                            new ProvenancePathFactoryImpl(), new HashCalculatorSHA256()
                    );
                });
        return finder.datasetFor(namespace);
    }

    @Before
    public void startGraphDb() throws IOException {
        nodeFactory = createNodeFactory();
        try (Transaction tx = getGraphDb().beginTx()) {
            getTaxonIndex();
            tx.commit();
        }
    }

    protected NodeFactoryNeo4j getNodeFactory() {
        if (nodeFactory == null) {
            nodeFactory = createNodeFactory();
        }
        return nodeFactory;
    }


    protected TaxonIndex getTaxonIndex() {
        if (taxonIndex == null) {
            taxonIndex = new NonResolvingTaxonIndex(getGraphDb());
        }
        return taxonIndex;
    }


    protected GraphServiceFactory getGraphFactory() {
        return new GraphServiceFactory() {

            @Override
            public GraphDatabaseService getGraphService() {
                return neo4j.defaultDatabaseService();
            }

            @Override
            public void close() {
            }
        };
    }

    protected void importStudy(DatasetImporter importer) throws StudyImporterException {
        importer.importStudy();
        resolveNames();
    }

    protected void resolveNames() {
        new NameResolver(
                getGraphFactory(),
                getNodeIdCollector(),
                getTaxonIndex()
        ).index();
    }

    protected TermLookupService getTermLookupService() {
        return new TestTermLookupService();
    }

    protected TermLookupService getEnvoLookupService() {
        return new TestTermLookupService();
    }


    private static class TestTermLookupService implements TermLookupService {
        @Override
        public List<Term> lookupTermByName(final String name) throws TermLookupServiceException {
            return new ArrayList<Term>() {{
                add(new TermImpl("TEST:" + name, name));
            }};
        }
    }

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
