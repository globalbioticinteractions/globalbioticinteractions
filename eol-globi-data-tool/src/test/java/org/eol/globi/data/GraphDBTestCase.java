package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.taxon.TaxonIndexImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderException;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.junit.After;
import org.junit.Before;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class GraphDBTestCase {

    private GraphDatabaseService graphDb;

    protected NodeFactory nodeFactory;

    @Before
    public void startGraphDb() throws IOException {
        graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase();
        PropertyEnricher enricher = new PassThroughEnricher();
        nodeFactory = createNodeFactory(enricher);
    }

    protected NodeFactory createNodeFactory(PropertyEnricher enricher) {
        NodeFactoryImpl nodeFactoryImpl = new NodeFactoryImpl(getGraphDb(), new TaxonIndexImpl(enricher,
                new PassThroughCorrectionService(), getGraphDb()));
        nodeFactoryImpl.setEcoregionFinder(new EcoregionFinder() {

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
        nodeFactoryImpl.setEnvoLookupService(getEnvoLookupService());
        nodeFactoryImpl.setTermLookupService(getTermLookupService());
        nodeFactoryImpl.setDoiResolver(new DOIResolver() {
            @Override
            public String findDOIForReference(String reference) throws IOException {
                return StringUtils.isBlank(reference) ? null : "doi:" + reference;
            }

            @Override
            public String findCitationForDOI(String doi) throws IOException {
                return StringUtils.isBlank(doi) ? null : "citation:" + doi;
            }
        });

        return nodeFactoryImpl;
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

    protected GraphDatabaseService getGraphDb() {
        return graphDb;
    }

    private static class TestTermLookupService implements TermLookupService {
        @Override
        public List<Term> lookupTermByName(final String name) throws TermLookupServiceException {
            return new ArrayList<Term>() {{
                add(new Term("TEST:" + name, name));
            }};
        }
    }

}
