package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.taxon.CorrectionService;
import org.eol.globi.data.taxon.TaxonServiceImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.Ecoregion;
import org.eol.globi.geo.EcoregionFinder;
import org.eol.globi.geo.EcoregionFinderException;
import org.eol.globi.service.DOIResolver;
import org.eol.globi.service.TaxonEnricher;
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
        final TaxonEnricher taxonEnricher = new TaxonEnricher() {

            @Override
            public void enrich(Taxon taxon) {

            }
        };
        nodeFactory = new NodeFactory(graphDb, new TaxonServiceImpl(taxonEnricher, new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return taxonName;
            }
        }, getGraphDb()));
        nodeFactory.setEcoregionFinder(new EcoregionFinder() {

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
        nodeFactory.setEnvoLookupService(new TestTermLookupService());
        nodeFactory.setTermLookupService(new TestTermLookupService());
        nodeFactory.setDoiResolver(new DOIResolver() {
            @Override
            public String findDOIForReference(String reference) throws IOException {
                return StringUtils.isBlank(reference) ? null : "doi:" + reference;
            }

            @Override
            public String findCitationForDOI(String doi) throws IOException {
                return StringUtils.isBlank(doi) ? null : "citation:" + doi;
            }
        });

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
