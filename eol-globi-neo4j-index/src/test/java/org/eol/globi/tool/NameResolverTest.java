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
import org.eol.globi.service.PropertyEnricherSingle;
import org.eol.globi.service.TaxonUtil;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;

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
        assertResolveNames(RelTypes.COLLECTED, getGraphDb());
    }

    @Test
    public void doNameResolvingForRefuting() throws NodeFactoryException {
        assertResolveNames(RelTypes.REFUTES, getGraphDb());
    }

    @Test
    public void doNameResolvingForSupporting() throws NodeFactoryException {
        assertResolveNames(RelTypes.SUPPORTS, getGraphDb());
    }

    private void assertResolveNames(RelTypes relTypes, final GraphDatabaseService graphDb) throws NodeFactoryException {
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

        final NameResolver nameResolver = new NameResolver(factory, getNodeIdCollector(), getTaxonIndex());
        nameResolver.setBatchSize(1L);

        nameResolver.index();

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
        StudyImpl bla = new StudyImpl("bla", null, null);
        Specimen someOtherOrganism = nodeFactory.createSpecimen(bla, new TaxonImpl("Blaus bla", "INAT_TAXON:58831"));
        Specimen someOtherOrganism2 = nodeFactory.createSpecimen(bla, new TaxonImpl("Redus rha", "INAT_TAXON:126777"));
        someOtherOrganism.ate(someOtherOrganism2);

        GraphServiceFactory graphServiceFactory = new GraphServiceFactoryProxy(getGraphDb());

        final NameResolver nameResolver = new NameResolver(graphServiceFactory, getNodeIdCollector(), getTaxonIndex());
        nameResolver.setBatchSize(1L);
        nameResolver.index();

        Taxon resolvedTaxon = taxonIndex.findTaxonById("INAT_TAXON:58831");
        assertThat(resolvedTaxon, is(notNullValue()));
        assertThat(resolvedTaxon.getExternalId(), is("INAT_TAXON:58831"));
        assertThat(resolvedTaxon.getName(), is("Blaus bla"));
        Taxon resolvedTaxon2 = taxonIndex.findTaxonByName("Blaus bla");
        assertThat(resolvedTaxon2, is(notNullValue()));
        assertThat(resolvedTaxon2.getExternalId(), is("INAT_TAXON:58831"));
    }

    @Test
    public void iNaturalistTaxonWikidata() throws NodeFactoryException {
        TaxonImpl taxon = new TaxonImpl("Ficus", "INAT_TAXON:50999");
        taxon.setPath("Plantae | Tracheophyta | Magnoliopsida | Rosales | Moraceae | Ficus");
        taxon.setPathNames("kingdom | phylum | class | order | family | genus");
        TaxonImpl taxon1 = new TaxonImpl("Ficus", "INAT_TAXON:208863");
        taxon1.setPath("Animalia | Mollusca | Gastropoda | Littorinimorpha | Ficidae | Ficus");
        taxon1.setPathNames("kingdom | phylum | class | order | family | genus");
        TaxonImpl taxon2 = new TaxonImpl("Ficus", "12345");
        TaxonImpl taxon3 = new TaxonImpl("Ficus", "WD:Q1938846");
        Study study = new StudyImpl("bla", null, null);
        Specimen someOtherOrganism = nodeFactory.createSpecimen(study, taxon);
        Specimen someOtherOrganism2 = nodeFactory.createSpecimen(study, taxon1);
        Specimen someOtherOrganism3 = nodeFactory.createSpecimen(study, taxon2);
        Specimen someOtherOrganism4 = nodeFactory.createSpecimen(study, taxon3);
        someOtherOrganism.ate(someOtherOrganism2);
        someOtherOrganism.ate(someOtherOrganism3);
        someOtherOrganism.ate(someOtherOrganism4);

        GraphServiceFactory graphServiceFactory = new GraphServiceFactoryProxy(getGraphDb());
        ResolvingTaxonIndex taxonIndexNew = new org.eol.globi.taxon.ResolvingTaxonIndex(new PropertyEnricherNoop(), getGraphDb());
        taxonIndexNew.setIndexResolvedTaxaOnly(false);

        final NameResolver nameResolver = new NameResolver(
                graphServiceFactory,
                getNodeIdCollector(),
                taxonIndexNew
        );
        nameResolver.setBatchSize(1L);
        nameResolver.index();

        Taxon resolvedTaxon = taxonIndexNew.findTaxonById("INAT_TAXON:50999");
        assertThat(resolvedTaxon, is(notNullValue()));
        assertThat(resolvedTaxon.getExternalId(), is("INAT_TAXON:50999"));
        assertThat(resolvedTaxon.getName(), is("Ficus"));
        assertThat(resolvedTaxon.getPath(), is("Plantae | Tracheophyta | Magnoliopsida | Rosales | Moraceae | Ficus"));

        resolvedTaxon = taxonIndexNew.findTaxonById("INAT_TAXON:208863");
        assertThat(resolvedTaxon, is(notNullValue()));
        assertThat(resolvedTaxon.getExternalId(), is("INAT_TAXON:208863"));
        assertThat(resolvedTaxon.getName(), is("Ficus"));
        assertThat(resolvedTaxon.getPath(), is("Animalia | Mollusca | Gastropoda | Littorinimorpha | Ficidae | Ficus"));
        Taxon resolvedTaxon2 = taxonIndex.findTaxonByName("Ficus");
        assertThat(resolvedTaxon2, is(notNullValue()));
        assertThat(resolvedTaxon2.getExternalId(), is("INAT_TAXON:50999"));

        TaxonImpl taxon4 = new TaxonImpl("Ficus");
        taxon4.setPath("Animalia | Mollusca | Gastropoda | Littorinimorpha | Ficidae | Ficus");
        taxon4.setPathNames("kingdom | phylum | class | order | family | genus");
        Taxon taxonMollusk = taxonIndexNew.getOrCreateTaxon(taxon4);
        assertThat(taxonMollusk.getPath(), is("Animalia | Mollusca | Gastropoda | Littorinimorpha | Ficidae | Ficus"));
    }

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
        org.eol.globi.taxon.ResolvingTaxonIndex resolvingIndex
                = new org.eol.globi.taxon.ResolvingTaxonIndex(enricher, getGraphDb());
        resolvingIndex.setIndexResolvedTaxaOnly(true);
        resolvingIndex.skipHomonymMatches(false);
        final NameResolver nameResolver = new NameResolver(
                new GraphServiceFactoryProxy(getGraphDb()),
                getNodeIdCollector(),
                resolvingIndex
        );

        nameResolver.setBatchSize(1L);
        nameResolver.index();

        Taxon resolvedTaxon0 = resolvingIndex.findTaxonById("foo:XXX");
        assertThat(resolvedTaxon0, is(notNullValue()));
        assertThat(resolvedTaxon0.getExternalId(), is("foo:XXX"));
        assertThat(resolvedTaxon0.getName(), is("Donald duckus"));

        Taxon resolvedTaxon = resolvingIndex.findTaxonById("foo:123");
        assertThat(resolvedTaxon, is(notNullValue()));
        assertThat(resolvedTaxon.getExternalId(), is("foo:XXX"));
        assertThat(resolvedTaxon.getName(), is("Donald duckus"));

        Taxon resolvedTaxon2 = resolvingIndex.findTaxonByName("foo");
        assertThat(resolvedTaxon2.getExternalId(), is("foo:XXX"));
        assertThat(resolvedTaxon2.getName(), is("Donald duckus"));
    }

    @Test
    public void progressMessage() {
        assertThat(NameResolver.getProgressMsg(10000L, 5555), is("[1800.18] taxon/s over [5.56] s"));
    }

}
