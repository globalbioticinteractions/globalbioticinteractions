package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherFactory;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.IndexHits;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class ResolvingTaxonIndexIT extends GraphDBTestCase {

    private ResolvingTaxonIndex taxonService;
    private static PropertyEnricher taxonEnricher = null;

    @BeforeClass
    public static void initEnricher() {
        taxonEnricher = PropertyEnricherFactory.createTaxonEnricher();
    }

    @AfterClass
    public static void shutdown() {
        taxonEnricher.shutdown();
    }

    @Before
    public void init() {
        this.taxonService = new ResolvingTaxonIndex(taxonEnricher, new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return taxonName;
            }
        }, getGraphDb());
    }

    @Test
    public void createTaxonFish() throws NodeFactoryException {
        taxonService.setCorrector(new CorrectionService() {
            @Override
            public String correct(String taxonName) {
                return StringUtils.equals("Fish", taxonName) ? "Actinopterygii" : taxonName;
            }
        });

        Taxon taxon2 = new TaxonImpl("Fish", null);
        taxon2.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon2);
        assertThat(taxon.getName(), is("Actinopterygii"));
        Taxon taxon1 = new TaxonImpl("Fish", null);
        taxon1.setPath(null);
        taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon.getName(), is("Actinopterygii"));

        assertThat(taxonService.findTaxonByName("Fish"), is(Matchers.notNullValue()));
        assertThat(taxonService.findCloseMatchesForTaxonName("Fish"), is(Matchers.notNullValue()));
    }

    @Test
    public void eolIdsThatPointToSinglePage() throws NodeFactoryException {
        String redirectTarget = "EOL:1073676";
        Taxon taxon2 = new TaxonImpl(null, redirectTarget);
        taxon2.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon2);
        String redirectSource = "EOL:10890298";
        Taxon taxon1 = new TaxonImpl(null, redirectSource);
        taxon1.setPath(null);
        TaxonNode otherTaxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(otherTaxon.getName(), is(taxon.getName()));
        assertThat(otherTaxon.getPath(), is(taxon.getPath()));
        assertThat(taxon.getUnderlyingNode().getId(), is(otherTaxon.getUnderlyingNode().getId()));

        assertThat(taxonService.findTaxonById(redirectTarget), is(notNullValue()));
    }

    @Test
    public void prosopisPlantAndInsect() throws NodeFactoryException {
        Taxon taxon2 = new TaxonImpl("Prosopis", null);
        taxon2.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon2);
        assertThat(taxon.getPath(), containsString("Plantae"));
        Taxon taxon1 = new TaxonImpl(null, "EOL:12072283");
        taxon1.setPath(null);
        TaxonNode otherTaxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(otherTaxon.getName(), is("Prosopis"));
        assertThat(otherTaxon.getExternalId(), is("EOL:12072283"));
        assertThat(otherTaxon.getPath(), containsString("Insecta"));
    }

    @Test
    public void createNoMatch() throws NodeFactoryException {
        taxonService.setEnricher(taxonEnricher);
        Taxon taxon1 = new TaxonImpl("Santa Claus meets Superman", null);
        taxon1.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon.getName(), is("Santa Claus meets Superman"));
        assertThat(taxon.getExternalId(), is("no:match"));
        assertThat(taxon.getPath(), is(nullValue()));
        assertThat(taxon.getCommonNames(), is(nullValue()));

        assertZeroHits(taxonService, "no:match");
    }

    @Test
    public void noDuplicatesOnSynomyms() throws NodeFactoryException {
        TaxonNode first = taxonService.getOrCreateTaxon(new TaxonImpl("Galeichthys felis"));
        TaxonNode second = taxonService.getOrCreateTaxon(new TaxonImpl("Ariopsis felis"));
        TaxonNode third = taxonService.getOrCreateTaxon(new TaxonImpl("Arius felis"));
        assertThat(first.getNodeID(), is(second.getNodeID()));
        assertThat(third.getNodeID(), is(second.getNodeID()));
        assertThat(third.getPath(), is("Animalia | Chordata | Actinopterygii | Siluriformes | Ariidae | Ariopsis | Ariopsis felis"));
    }

    @Test
    public void noDuplicatesOnChoppingNames() throws NodeFactoryException {
        TaxonNode first = taxonService.getOrCreateTaxon(new TaxonImpl("Ariopsis felis"));
        TaxonNode second = taxonService.getOrCreateTaxon(new TaxonImpl("Ariopsis felis something"));
        assertThat(first.getExternalId(), is(second.getExternalId()));
        assertThat(first.getNodeID(), is(second.getNodeID()));
        assertThat(taxonService.findTaxonByName("Ariopsis felis").getNodeID(), is(second.getNodeID()));
    }

    @Test
    public void firstGenusThenSpecies() throws NodeFactoryException {
        TaxonNode first = taxonService.getOrCreateTaxon(new TaxonImpl("Ariopsis"));
        TaxonNode second = taxonService.getOrCreateTaxon(new TaxonImpl("Ariopsis felis"));
        assertThat(first.getExternalId(), not(is(second.getExternalId())));
        assertThat(first.getNodeID(), not(is(second.getNodeID())));
        assertThat(taxonService.findTaxonByName("Ariopsis felis").getNodeID(), is(second.getNodeID()));
        assertThat(taxonService.findTaxonByName("Ariopsis").getNodeID(), is(first.getNodeID()));
    }

    @Test(expected = NodeFactoryException.class)
    public void nameTooShort() throws NodeFactoryException {
        taxonService.getOrCreateTaxon(new TaxonImpl(""));
    }

    @Test
    public void nameTooShortButHasExternalId() throws NodeFactoryException {
        TaxonNode taxon = taxonService.getOrCreateTaxon(new TaxonImpl("", "EOL:327955"));
        assertThat(taxon.getPath(), containsString("Homo sapiens"));
    }

    private void assertZeroHits(ResolvingTaxonIndex taxonService, String taxonName) {
        IndexHits<Node> hits = taxonService.findCloseMatchesForTaxonName(taxonName);
        assertThat(hits.size(), is(0));
    }
}
