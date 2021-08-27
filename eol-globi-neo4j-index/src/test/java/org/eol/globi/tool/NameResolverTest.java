package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.GraphDatabaseServiceBatchingTransactions;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.NonResolvingTaxonIndex;
import org.eol.globi.taxon.ResolvingTaxonIndex;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

public class NameResolverTest extends GraphDBTestCase {

    @Test
    public void doNameResolving() throws NodeFactoryException, PropertyEnricherException {
        assertResolveNames(RelTypes.COLLECTED, getGraphDb());
    }

    @Test
    public void doNameResolvingBatching() throws NodeFactoryException, PropertyEnricherException {
        GraphDatabaseServiceBatchingTransactions graphDb = new GraphDatabaseServiceBatchingTransactions(getGraphDb());
        graphDb.setBatchSize(2);
        assertResolveNames(RelTypes.COLLECTED, graphDb);
    }

    @Test
    public void doNameResolvingForRefuting() throws NodeFactoryException, PropertyEnricherException {
        assertResolveNames(RelTypes.REFUTES, getGraphDb());
    }

    @Test
    public void doNameResolvingForSupporting() throws NodeFactoryException, PropertyEnricherException {
        assertResolveNames(RelTypes.SUPPORTS, getGraphDb());
    }

    private void assertResolveNames(RelTypes relTypes, final GraphDatabaseService graphDb) throws NodeFactoryException {
        Specimen human = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("Homo sapiens", "NCBI:9606"), relTypes);
        Specimen animal = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("Animalia", "WORMS:2"), relTypes);
        human.ate(animal);
        Specimen fish = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("Arius felis", "WORMS:158711"), relTypes);
        human.ate(fish);

        assertNull(taxonIndex.findTaxonById("NCBI:9606"));
        assertNull(taxonIndex.findTaxonByName("Homo sapiens"));

        final NameResolver nameResolver = new NameResolver(new NonResolvingTaxonIndex(graphDb));
        nameResolver.setBatchSize(1L);

        final GraphServiceFactory factory = new GraphServiceFactory() {

            @Override
            public GraphDatabaseService getGraphService() {
                return getGraphDb();
            }

            @Override
            public void close() {

            }
        };
        nameResolver.index(factory);

        assertAnimalia(taxonIndex.findTaxonById("WORMS:2"));

        assertThat(taxonIndex.findTaxonByName("Arius felis"), is(notNullValue()));

        Taxon homoSapiens = taxonIndex.findTaxonByName("Homo sapiens");
        assertNotNull(homoSapiens);
        assertThat(homoSapiens.getExternalId(), is("NCBI:9606"));
    }

    public void assertAnimalia(Taxon animalia) {
        assertNotNull(animalia);
        assertThat(animalia.getName(), containsString("Animalia"));
    }

    @Test
    public void iNaturalistTaxon() throws NodeFactoryException {
        Specimen someOtherOrganism = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("Blaus bla", "INAT_TAXON:58831"));
        Specimen someOtherOrganism2 = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("bla", null, null)), new TaxonImpl("Redus rha", "INAT_TAXON:126777"));
        someOtherOrganism.ate(someOtherOrganism2);

        final NameResolver nameResolver = new NameResolver(new NonResolvingTaxonIndex(getGraphDb()));
        nameResolver.setBatchSize(1L);
        nameResolver.index(new GraphServiceFactory() {
            @Override
            public GraphDatabaseService getGraphService() {
                return getGraphDb();
            }

            @Override
            public void close() {

            }
        });

        Taxon resolvedTaxon = taxonIndex.findTaxonById("INAT_TAXON:58831");
        assertThat(resolvedTaxon, is(notNullValue()));
        assertThat(resolvedTaxon.getExternalId(), is("INAT_TAXON:58831"));
        assertThat(resolvedTaxon.getName(), is("Blaus bla"));
        Taxon resolvedTaxon2 = taxonIndex.findTaxonByName("Blaus bla");
        assertThat(resolvedTaxon2, is(notNullValue()));
        assertThat(resolvedTaxon2.getExternalId(), is("INAT_TAXON:58831"));
    }

    @Test
    public void literatureTaxon() throws NodeFactoryException {
        Specimen someOtherOrganism = nodeFactory.createSpecimen(nodeFactory.createStudy(
                new StudyImpl("bla", null, null)),
                new TaxonImpl("foo", "foo:123"));

        Specimen someOtherOrganism2 = nodeFactory.createSpecimen(nodeFactory.createStudy(
                new StudyImpl("bla", null, null)),
                new TaxonImpl("bar", "bar:456"));


        someOtherOrganism.ate(someOtherOrganism2);

        final NameResolver nameResolver = new NameResolver(new ResolvingTaxonIndex(new PropertyEnricher() {
            @Override
            public Map<String, String> enrichFirstMatch(Map<String, String> properties) throws PropertyEnricherException {
                return enrichAllMatches(properties).get(0);
            }

            @Override
            public List<Map<String, String>> enrichAllMatches(Map<String, String> properties) throws PropertyEnricherException {
                TaxonImpl literature = new TaxonImpl("doi:10.678/901", "doi:10.678/901");
                literature.setPath("some | other | path");

                TaxonImpl concept = new TaxonImpl("Donald duckus", "foo:XXX");
                concept.setPath("some | path");

                return Arrays.asList(TaxonUtil.taxonToMap(literature), TaxonUtil.taxonToMap(concept));
            }

            @Override
            public void shutdown() {

            }
        }, getGraphDb()));


        nameResolver.setBatchSize(1L);
        nameResolver.index(new GraphServiceFactory() {
            @Override
            public GraphDatabaseService getGraphService() {
                return getGraphDb();
            }

            @Override
            public void close() {

            }
        });

        Taxon resolvedTaxon = taxonIndex.findTaxonById("foo:123");
        assertThat(resolvedTaxon, is(notNullValue()));
        assertThat(resolvedTaxon.getExternalId(), is("foo:XXX"));
        assertThat(resolvedTaxon.getName(), is("Donald duckus"));

        Taxon resolvedTaxon2 = taxonIndex.findTaxonByName("foo");
        assertThat(resolvedTaxon2.getExternalId(), is("foo:XXX"));
        assertThat(resolvedTaxon2.getName(), is("Donald duckus"));
    }

    @Test
    public void progressMessage() {
        assertThat(NameResolver.getProgressMsg(10000L, 5555), is("[1800.18] taxon/s over [5.56] s"));
    }

}
