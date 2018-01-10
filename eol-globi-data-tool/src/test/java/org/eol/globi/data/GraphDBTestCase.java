package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderException;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetFinder;
import org.eol.globi.service.DatasetFinderException;
import org.eol.globi.service.DatasetFinderGitHubArchiveMaster;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.taxon.NonResolvingTaxonIndex;
import org.eol.globi.tool.NameResolver;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.DatasetFinderWithCache;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public abstract class GraphDBTestCase {

    private GraphDatabaseService graphDb;

    protected NodeFactory nodeFactory;

    protected TaxonIndex taxonIndex;

    public static Study getStudySingleton(GraphDatabaseService graphService) {
        List<Study> allStudies = NodeUtil.findAllStudies(graphService);
        assertThat(allStudies.size(), is(1));
        Study study = allStudies.get(0);
        assertNotNull(study);
        return study;
    }

    public static Dataset datasetFor(String namespace) throws DatasetFinderException {
        DatasetFinder finder = new DatasetFinderWithCache(new DatasetFinderGitHubArchiveMaster());
        return finder.datasetFor(namespace);
    }

    @Before
    public void startGraphDb() throws IOException {
        nodeFactory = createNodeFactory();
        getOrCreateTaxonIndex();
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
        if (graphDb == null) {
            graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        }
        return graphDb;
    }

    protected void importStudy(StudyImporter importer) throws StudyImporterException {
        importer.importStudy();
        resolveNames();
    }

    protected void resolveNames() {
        new NameResolver(getGraphDb(), getOrCreateTaxonIndex()).resolve();
    }


    NodeFactory createNodeFactory() {
        NodeFactoryNeo4j nodeFactoryNeo4j = new NodeFactoryNeo4j(getGraphDb());
        nodeFactoryNeo4j.setEcoregionFinder(new EcoregionFinder() {

            @Override
            public Collection<Ecoregion> findEcoregion(double lat, double lng) throws EcoregionFinderException {
                final Ecoregion ecoregion = new Ecoregion();
                ecoregion.setName("some eco region");
                ecoregion.setPath("some | eco | region | path");
                ecoregion.setId("some:id");
                ecoregion.setGeometry("POINT(0,0)");
                return new ArrayList<Ecoregion>() {{
                    add(ecoregion);
                }};
            }

            @Override
            public void shutdown() {

            }
        });
        nodeFactoryNeo4j.setEnvoLookupService(getEnvoLookupService());
        nodeFactoryNeo4j.setTermLookupService(getTermLookupService());
        return nodeFactoryNeo4j;
    }

    protected TermLookupService getTermLookupService() {
        return new TestTermLookupService();
    }

    protected TermLookupService getEnvoLookupService() {
        return new TestTermLookupService();
    }

    @After
    public void shutdownGraphDb() {
        graphDb.shutdown();
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
