package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherFactory;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
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
        assertThat(taxonService.findTaxonById(redirectSource).getUnderlyingNode().getId(), is(taxon.getUnderlyingNode().getId()));
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
    public void createMatchAnimalRemains() throws NodeFactoryException {
        taxonService.setEnricher(taxonEnricher);
        taxonService.setCorrector(new TaxonNameCorrector());
        Taxon taxon1 = new TaxonImpl("Animal remains", null);
        taxon1.setPath(null);
        TaxonNode firstTaxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(firstTaxon.getName(), is("Animalia"));
        assertThat(firstTaxon.getExternalId(), is("EOL:1"));

        TaxonNode secondTaxon = taxonService.findTaxonByName("Animal remains");
        assertThat(secondTaxon.getNodeID(), is(firstTaxon.getNodeID()));

        TaxonNode thirdTaxon = taxonService.getOrCreateTaxon(new TaxonImpl("Animal remains"));
        assertThat(thirdTaxon.getNodeID(), is(firstTaxon.getNodeID()));
    }

    @Test
    public void externalIdNoPath() throws NodeFactoryException {
        taxonService.setEnricher(taxonEnricher);
        taxonService.setCorrector(new TaxonNameCorrector());
        TaxonNode firstTaxon = taxonService.getOrCreateTaxon(new TaxonImpl(null, "EOL:3764974"));
        assertThat(firstTaxon.getName(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(firstTaxon.getPath(), is(nullValue()));
        TaxonNode secondTaxon = taxonService.getOrCreateTaxon(new TaxonImpl(null, "EOL:3764974"));
        assertThat(secondTaxon.getNodeID(), is(firstTaxon.getNodeID()));
    }

    @Test
    public void externalIdDummyName() throws NodeFactoryException {
        taxonService.setEnricher(taxonEnricher);
        taxonService.setCorrector(new TaxonNameCorrector());
        TaxonNode taxon = taxonService.getOrCreateTaxon(new TaxonImpl(null, "EOL:1"));
        assertThat(taxon.getName(), is("Animalia"));
        assertThat(taxon.getExternalId(), is("EOL:1"));
        assertThat(taxon.getPath(), containsString("Animalia"));
        assertThat(taxon.getCommonNames(), containsString("animals"));

        TaxonNode secondTaxon = taxonService.getOrCreateTaxon(new TaxonImpl(null, "EOL:1"));
        assertThat(secondTaxon.getNodeID(), is(taxon.getNodeID()));

        TaxonNode animaliaTaxon = taxonService.findTaxonById("EOL:1");
        assertThat(animaliaTaxon, is(Matchers.notNullValue()));
        assertThat(animaliaTaxon.getName(), is("Animalia"));

        animaliaTaxon = taxonService.findTaxonByName("Animalia");
        assertThat(animaliaTaxon, is(Matchers.notNullValue()));
        assertThat(animaliaTaxon.getName(), is("Animalia"));
    }

    @Test
    public void checkBugDuplicateEntryBioInfo() throws NodeFactoryException {
        // this name was causing problem end of Jan 2014 in BioInfo dataset:
        // Caused by: org.eol.globi.data.NodeFactoryException: found duplicate taxon for [Exidia plana] (original name: [Exidia plana]).
        taxonService.setCorrector(new TaxonNameCorrector());
        String taxonName = "Exidia plana";
        assertThat(taxonService.findTaxonByName(taxonName), is(nullValue()));
        taxonService.getOrCreateTaxon(new TaxonImpl(taxonName));
        taxonService.getOrCreateTaxon(new TaxonImpl(taxonName + " bla"));
        taxonService.getOrCreateTaxon(new TaxonImpl("Exidia nigricans"));
        assertThat(taxonService.findTaxonByName(taxonName), is(notNullValue()));
    }

    @Test
    public void checkBugDuplicateEntryFerrerParis() throws NodeFactoryException {
        // this name was causing problem end of Jan 2014 in BioInfo dataset:
        // Caused by: org.eol.globi.data.NodeFactoryException: found duplicate taxon for [Exidia glandulosa] (original name: [Exidia plana])
        taxonService.setCorrector(new TaxonNameCorrector());
        taxonService.getOrCreateTaxon(new TaxonImpl("Exidia glandulosa"));
        assertThat(taxonService.findTaxonByName("Exidia plana"), is(nullValue()));
        taxonService.getOrCreateTaxon(new TaxonImpl("Exidia plana"));
        taxonService.getOrCreateTaxon(new TaxonImpl("Exidia plana" + " bla"));
        // note that at time of writing (Feb 2014) EOL considers Exidia plana a alternate name of Exidia glandulosa
        assertThat(taxonService.findTaxonByName("Exidia glandulosa"), is(notNullValue()));
        assertThat(taxonService.findTaxonByName("Exidia plana"), is(notNullValue()));
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

    @Test
    public void noDuplicatesOnAlternateNames() throws NodeFactoryException {
        TaxonNode first = taxonService.getOrCreateTaxon(new TaxonImpl("Cliona caribbaea"));
        TaxonNode second = taxonService.getOrCreateTaxon(new TaxonImpl("Cliona langae"));
        assertThat(first.getExternalId(), is(second.getExternalId()));
        assertThat(first.getPath(), is(second.getPath()));
        assertThat(first.getNodeID(), is(second.getNodeID()));
        assertThat(first.getExternalId(), is(second.getExternalId()));

        TaxonNode taxon = taxonService.findTaxonByName("Cliona langae");
        assertThat(taxon, is(notNullValue()));
        taxon = taxonService.findTaxonByName("Cliona caribbaea");
        assertThat(taxon, is(notNullValue()));

        assertThat(taxon.getNodeID(), is(first.getNodeID()));
        assertThat(first.getNodeID(), is(second.getNodeID()));
    }

    @Test
    public void noDuplicatesOnCircularSuggestionNames() throws NodeFactoryException {
        // Mimesa bicolor -> Mimesa equestris -> Memisa bicolor
        taxonService.setCorrector(new TaxonNameCorrector());
        Taxon taxon3 = new TaxonImpl("Mimesa bicolor foo", null);
        taxon3.setPath(null);
        TaxonNode bicolor = taxonService.getOrCreateTaxon(taxon3);
        assertThat(taxonService.findTaxonByName("Mimesa bicolor"), is(notNullValue()));
        Taxon taxon2 = new TaxonImpl("Mimesa bicolor", null);
        taxon2.setPath(null);
        taxonService.getOrCreateTaxon(taxon2);
        Taxon taxon1 = new TaxonImpl("Mimesa equestris bla", null);
        taxon1.setPath(null);
        TaxonNode equestris = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxonService.findTaxonByName("Mimesa equestris"), is(notNullValue()));
        taxonService.getOrCreateTaxon(new TaxonImpl("Mimesa equestris"));
        assertThat(taxonService.findTaxonByName("Mimesa equestris bla").getNodeID(), is(equestris.getNodeID()));
        assertThat(bicolor.getNodeID(), not(Is.is(equestris.getNodeID())));
    }

    @Test
    public void specialCharacters() throws NodeFactoryException {
        taxonService.setCorrector(new TaxonNameCorrector());
        Taxon taxon2 = new TaxonImpl("Longspine swimming crab", null);
        taxon2.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon2);
        assertThat(taxon.getName(), is("Acheloüs spinicarpus"));
        assertThat(taxon.getExternalId(), is(notNullValue()));
        assertThat(taxon.getPath(), is(notNullValue()));

        Taxon taxon1 = new TaxonImpl("Portunus spinicarpus", null);
        taxon1.setPath(null);
        TaxonNode secondTaxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(secondTaxon.getNodeID(), is(taxon.getNodeID()));
        assertThat(secondTaxon.getName(), is("Acheloüs spinicarpus"));

        assertThat(taxonService.findTaxonByName("Acheloüs spinicarpus"), is(notNullValue()));
        assertThat(taxonService.findTaxonByName("Portunus spinicarpus"), is(notNullValue()));
    }

    @Test
    public void createHomoSapiens() throws NodeFactoryException {
        Taxon taxon1 = new TaxonImpl("Homo sapiens", null);
        taxon1.setPath(null);
        TaxonNode taxon = taxonService.getOrCreateTaxon(taxon1);
        assertThat(taxon.getName(), is("Homo sapiens"));
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Primates"));
        assertThat(taxon.getCommonNames(), containsString("Humans"));
        assertThat(taxon.getPathNames(), containsString("genus"));
        assertZeroHits(taxonService, "no:match");
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
