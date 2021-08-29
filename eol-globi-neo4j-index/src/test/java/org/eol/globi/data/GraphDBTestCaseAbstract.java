package org.eol.globi.data;

import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.db.GraphServiceUtil;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.taxon.NonResolvingTaxonIndex;
import org.eol.globi.tool.NameResolver;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetRegistry;
import org.globalbioticinteractions.dataset.DatasetRegistryException;
import org.globalbioticinteractions.dataset.DatasetRegistryWithCache;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;

public abstract class GraphDBTestCaseAbstract {

    private GraphServiceFactory graphFactory;

    protected NodeFactory nodeFactory;

    protected TaxonIndex taxonIndex;

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
                    public Collection<String> findNamespaces() throws DatasetRegistryException {
                        return Collections.emptyList();
                    }

                    @Override
                    public Dataset datasetFor(String namespace) throws DatasetRegistryException {
                        return new DatasetImpl(namespace, URI.create("some:uri"), in -> in);
                    }
                },
                dataset -> CacheUtil.cacheFor(dataset.getNamespace(),
                        "target/datasets",
                        inStream -> inStream));
        return finder.datasetFor(namespace);
    }

    @Before
    public void startGraphDb() throws IOException {
        nodeFactory = createNodeFactory();
        try(Transaction tx = getGraphDb().beginTx()) {
            getOrCreateTaxonIndex();
            tx.success();
        }
    }

    public void afterGraphDBStart() {
    }

    public void beforeGraphDbShutdown() {
    }

    @After
    public void shutdownGraphDb() throws Exception {
        beforeGraphDbShutdown();
        graphFactory.close();
    }

    protected NodeFactoryNeo4j getNodeFactory() {
        return (NodeFactoryNeo4j) nodeFactory;
    }


    protected TaxonIndex getOrCreateTaxonIndex() {
        if (taxonIndex == null) {
            taxonIndex = new NonResolvingTaxonIndex(getGraphDb());
        }
        return taxonIndex;
    }

    protected GraphDatabaseService getGraphDb() {
        if (graphFactory == null) {
            graphFactory = getGraphFactory();
            afterGraphDBStart();
        }
        return graphFactory.getGraphService();
    }

    protected GraphServiceFactory getGraphFactory() {
        return new GraphServiceFactory() {

            private GraphDatabaseService graphDb = null;

            @Override
            public GraphDatabaseService getGraphService() {
                GraphDatabaseService graphDb = this.graphDb;
                GraphServiceUtil.verifyState(graphDb);

                if (this.graphDb == null) {
                    this.graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
                }
                return this.graphDb;
            }

            @Override
            public void close() {
                if (graphDb != null) {
                    graphDb.shutdown();
                    graphDb = null;
                }
            }
        };
    }

    protected void importStudy(DatasetImporter importer) throws StudyImporterException {
        importer.importStudy();
        resolveNames();
    }

    protected void resolveNames() {
        new NameResolver(getOrCreateTaxonIndex()).index(new GraphServiceFactoryProxy(getGraphDb()));
    }


    abstract protected NodeFactory createNodeFactory();

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

}
