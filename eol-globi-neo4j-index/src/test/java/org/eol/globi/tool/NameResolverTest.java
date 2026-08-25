package org.eol.globi.tool;

import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.PropertyEnricherNoop;
import org.eol.globi.data.ResolvingTaxonIndex;
import org.eol.globi.db.GraphServiceFactory;
import org.eol.globi.db.GraphServiceFactoryProxy;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.taxon.ResolvingTaxonIndexImpl;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;

public class NameResolverTest extends GraphDBTestCase {


    @Test
    public void doNameResolving() throws NodeFactoryException {
        assertResolveNames(RelTypes.COLLECTED);
    }

    @Test
    public void doNameResolvingForRefuting() throws NodeFactoryException {
        assertResolveNames(RelTypes.REFUTES);
    }

    @Test
    public void doNameResolvingForSupporting() throws NodeFactoryException {
        assertResolveNames(RelTypes.SUPPORTS);
    }

    private void assertResolveNames(RelTypes relTypes) throws NodeFactoryException {
        StudyImpl study = new StudyImpl("bla", null, null);
        Specimen human = nodeFactory.createSpecimen(
                study,
                new TaxonImpl("Homo sapiens", "NCBI:9606"),
                relTypes
        );

        Specimen animal = nodeFactory.createSpecimen(
                study,
                new TaxonImpl("Animalia", "WORMS:2"),
                relTypes
        );

        human.ate(animal);

        Specimen fish = nodeFactory.createSpecimen(
                study,
                new TaxonImpl("Arius felis", "WORMS:158711"),
                relTypes
        );

        human.ate(fish);

        final GraphServiceFactory factory = new GraphServiceFactory() {

            @Override
            public GraphDatabaseService getGraphService() {
                return getGraphDb();
            }

            @Override
            public void close() {

            }
        };

        final NameResolver nameResolver = new NameResolver(factory, getTaxonIndexFactory());
        nameResolver.setBatchSize(1L);

        nameResolver.index();

        try (Transaction tx = getGraphDb().beginTx()) {
            ResolvingTaxonIndex taxonIndex = getTaxonIndexFactory().create(tx);
            assertAnimalia(taxonIndex.findTaxonById("WORMS:2"));

            assertThat(taxonIndex.findTaxonByName("Arius felis"), is(notNullValue()));

            Taxon homoSapiens = taxonIndex.findTaxonByName("Homo sapiens");
            assertNotNull(homoSapiens);
            assertThat(homoSapiens.getExternalId(), is("NCBI:9606"));
            tx.commit();
        }
    }


    public void assertAnimalia(Taxon animalia) {
        assertNotNull(animalia);
        assertThat(animalia.getName(), containsString("Animalia"));
    }

    @Test
    public void iNaturalistTaxon() throws NodeFactoryException {
        StudyImpl bla = new StudyImpl("bla", null, null);
        Specimen someOtherOrganism = nodeFactory.createSpecimen(bla, new TaxonImpl("Blaus bla", "INAT_TAXON:58831"));
        Specimen someOtherOrganism2 = nodeFactory.createSpecimen(bla, new TaxonImpl("Redus rha", "INAT_TAXON:126777"));
        someOtherOrganism.ate(someOtherOrganism2);

        GraphServiceFactory graphServiceFactory = new GraphServiceFactoryProxy(getGraphDb());

        final NameResolver nameResolver = new NameResolver(graphServiceFactory, getTaxonIndexFactory());
        nameResolver.setBatchSize(1L);
        nameResolver.index();

        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon resolvedTaxon = getTaxonIndexFactory().create(tx).findTaxonById("INAT_TAXON:58831");
            assertThat(resolvedTaxon, is(notNullValue()));
            assertThat(resolvedTaxon.getExternalId(), is("INAT_TAXON:58831"));
            assertThat(resolvedTaxon.getName(), is("Blaus bla"));
            tx.commit();
        }

        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon resolvedTaxon2 = getTaxonIndexFactory().create(tx).findTaxonByName("Blaus bla");
            assertThat(resolvedTaxon2, is(notNullValue()));
            assertThat(resolvedTaxon2.getExternalId(), is("INAT_TAXON:58831"));
        }
    }


    @Ignore
    @Test
    public void literatureTaxon() throws NodeFactoryException {
        Specimen someOtherOrganism = nodeFactory.createSpecimen(
                new StudyImpl("bla", null, null),
                new TaxonImpl("foo", "foo:123"));

        Specimen someOtherOrganism2 = nodeFactory.createSpecimen(
                new StudyImpl("bla", null, null),
                new TaxonImpl("bar", "bar:456"));


        someOtherOrganism.ate(someOtherOrganism2);


        PropertyEnricher enricher = new PropertyEnricher() {
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
        };
        TaxonIndexFactory indexFactory = new TaxonIndexFactory() {
            @Override
            public ResolvingTaxonIndex create(Transaction tx) {
                ResolvingTaxonIndexImpl resolvingIndex
                        = new ResolvingTaxonIndexImpl(enricher, tx);
                resolvingIndex.setIndexResolvedTaxaOnly(true);
                resolvingIndex.skipHomonymMatches(false);
                return resolvingIndex;
            }
        };
        final NameResolver nameResolver = new NameResolver(
                new GraphServiceFactoryProxy(getGraphDb()),
                indexFactory
        );

        nameResolver.setBatchSize(1L);
        nameResolver.index();

        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon resolvedTaxon0 = indexFactory.create(tx).findTaxonById("foo:XXX");
            assertThat(resolvedTaxon0, is(notNullValue()));
            assertThat(resolvedTaxon0.getExternalId(), is("foo:XXX"));
            assertThat(resolvedTaxon0.getName(), is("Donald duckus"));
            tx.commit();
        }

        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon resolvedTaxon = indexFactory.create(tx).findTaxonById("foo:123");
            assertThat(resolvedTaxon, is(notNullValue()));
            assertThat(resolvedTaxon.getExternalId(), is("foo:XXX"));
            assertThat(resolvedTaxon.getName(), is("Donald duckus"));
            tx.commit();
        }

        try (Transaction tx = getGraphDb().beginTx()) {
            Taxon resolvedTaxon2 = indexFactory.create(tx).findTaxonByName("foo");
            assertThat(resolvedTaxon2.getExternalId(), is("foo:XXX"));
            assertThat(resolvedTaxon2.getName(), is("Donald duckus"));
            tx.commit();
        }
    }

    @Test
    public void progressMessage() {
        assertThat(NameResolver.getProgressMsg(10000L, 5555), is("in [5.56] s at [1800.18] taxon/s "));
    }

}
