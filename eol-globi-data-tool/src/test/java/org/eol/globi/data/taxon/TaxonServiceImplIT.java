package org.eol.globi.data.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.TaxonEnricher;
import org.eol.globi.service.TaxonEnricherFactory;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
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

public class TaxonServiceImplIT extends GraphDBTestCase {

    private TaxonServiceImpl taxonService;
    private static TaxonEnricher taxonEnricher = null;

    @BeforeClass
    public static void initEnricher() {
        taxonEnricher = TaxonEnricherFactory.createTaxonEnricher();
    }

    @Before
    public void init() {
        this.taxonService = new TaxonServiceImpl(taxonEnricher, new CorrectionService() {
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

        TaxonNode taxon = taxonService.getOrCreateTaxon("Fish", null, null);
        assertThat(taxon.getName(), is("Actinopterygii"));
        taxon = taxonService.getOrCreateTaxon("Fish", null, null);
        assertThat(taxon.getName(), is("Actinopterygii"));

        assertThat(taxonService.findTaxonByName("Fish"), is(Matchers.notNullValue()));
        assertThat(taxonService.findCloseMatchesForTaxonName("Fish"), is(Matchers.notNullValue()));
    }

    @Test
    public void eolIdsThatPointToSinglePage() throws NodeFactoryException {
        String redirectTarget = "EOL:1073676";
        TaxonNode taxon = taxonService.getOrCreateTaxon(null, redirectTarget, null);
        String redirectSource = "EOL:10890298";
        TaxonNode otherTaxon = taxonService.getOrCreateTaxon(null, redirectSource, null);
        assertThat(otherTaxon.getName(), is(taxon.getName()));
        assertThat(otherTaxon.getPath(), is(taxon.getPath()));
        assertThat(taxon.getUnderlyingNode().getId(), is(otherTaxon.getUnderlyingNode().getId()));

        assertThat(taxonService.findTaxonById(redirectTarget), is(notNullValue()));
        assertThat(taxonService.findTaxonById(redirectSource).getUnderlyingNode().getId(), is(taxon.getUnderlyingNode().getId()));
    }

    @Test
    public void prosopisPlantAndInsect() throws NodeFactoryException {
        TaxonNode taxon = taxonService.getOrCreateTaxon("Prosopis", null, null);
        assertThat(taxon.getPath(), containsString("Plantae"));
        TaxonNode otherTaxon = taxonService.getOrCreateTaxon(null, "EOL:12072283", null);
        assertThat(otherTaxon.getName(), is("Prosopis"));
        assertThat(otherTaxon.getExternalId(), is("EOL:12072283"));
        assertThat(otherTaxon.getPath(), containsString("Insecta"));
    }

    @Test
    public void createNoMatch() throws NodeFactoryException {
        taxonService.setEnricher(taxonEnricher);
        TaxonNode taxon = taxonService.getOrCreateTaxon("Santa Claus meets Superman", null, null);
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
        TaxonNode firstTaxon = taxonService.getOrCreateTaxon("Animal remains", null, null);
        assertThat(firstTaxon.getName(), is("Animalia"));
        assertThat(firstTaxon.getExternalId(), is("EOL:1"));

        TaxonNode secondTaxon = taxonService.findTaxonByName("Animal remains");
        assertThat(secondTaxon.getNodeID(), is(firstTaxon.getNodeID()));

        TaxonNode thirdTaxon = taxonService.getOrCreateTaxon("Animal remains", null, null);
        assertThat(thirdTaxon.getNodeID(), is(firstTaxon.getNodeID()));
    }

    @Test
    public void externalIdNoPath() throws NodeFactoryException {
        taxonService.setEnricher(taxonEnricher);
        taxonService.setCorrector(new TaxonNameCorrector());
        TaxonNode firstTaxon = taxonService.getOrCreateTaxon(null, "EOL:3764974", null);
        assertThat(firstTaxon.getName(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(firstTaxon.getPath(), is(nullValue()));
        TaxonNode secondTaxon = taxonService.getOrCreateTaxon(null, "EOL:3764974", null);
        assertThat(secondTaxon.getNodeID(), is(firstTaxon.getNodeID()));
    }

    @Test
    public void externalIdDummyName() throws NodeFactoryException {
        taxonService.setEnricher(taxonEnricher);
        taxonService.setCorrector(new TaxonNameCorrector());
        TaxonNode taxon = taxonService.getOrCreateTaxon(null, "EOL:1", null);
        assertThat(taxon.getName(), is("Animalia"));
        assertThat(taxon.getExternalId(), is("EOL:1"));
        assertThat(taxon.getPath(), containsString("Animalia"));
        assertThat(taxon.getCommonNames(), containsString("animals"));

        TaxonNode secondTaxon = taxonService.getOrCreateTaxon(null, "EOL:1", null);
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
        taxonService.getOrCreateTaxon(taxonName, null, null);
        taxonService.getOrCreateTaxon(taxonName + " bla", null, null);
        taxonService.getOrCreateTaxon("Exidia nigricans", null, null);
        assertThat(taxonService.findTaxonByName(taxonName), is(notNullValue()));
    }

    @Test
    public void checkBugDuplicateEntryFerrerParis() throws NodeFactoryException {
        // this name was causing problem end of Jan 2014 in BioInfo dataset:
        // Caused by: org.eol.globi.data.NodeFactoryException: found duplicate taxon for [Exidia glandulosa] (original name: [Exidia plana])
        taxonService.setCorrector(new TaxonNameCorrector());
        taxonService.getOrCreateTaxon("Exidia glandulosa", null, null);
        assertThat(taxonService.findTaxonByName("Exidia plana"), is(nullValue()));
        taxonService.getOrCreateTaxon("Exidia plana", null, null);
        taxonService.getOrCreateTaxon("Exidia plana" + " bla", null, null);
        // note that at time of writing (Feb 2014) EOL considers Exidia plana a alternate name of Exidia glandulosa
        assertThat(taxonService.findTaxonByName("Exidia glandulosa"), is(notNullValue()));
        assertThat(taxonService.findTaxonByName("Exidia plana"), is(notNullValue()));
    }

    @Test
    public void noDuplicatesOnSynomyms() throws NodeFactoryException {
        TaxonNode first = taxonService.getOrCreateTaxon("Galeichthys felis", null, null);
        TaxonNode second = taxonService.getOrCreateTaxon("Ariopsis felis", null, null);
        TaxonNode third = taxonService.getOrCreateTaxon("Arius felis", null, null);
        assertThat(first.getNodeID(), is(second.getNodeID()));
        assertThat(third.getNodeID(), is(second.getNodeID()));
        assertThat(third.getPath(), is("Animalia | Chordata | Actinopterygii | Siluriformes | Ariidae | Ariopsis | Ariopsis felis"));
    }

    @Test
    public void noDuplicatesOnChoppingNames() throws NodeFactoryException {
        TaxonNode first = taxonService.getOrCreateTaxon("Ariopsis felis", null, null);
        TaxonNode second = taxonService.getOrCreateTaxon("Ariopsis felis something", null, null);
        assertThat(first.getExternalId(), is(second.getExternalId()));
        assertThat(first.getNodeID(), is(second.getNodeID()));
        assertThat(taxonService.findTaxonByName("Ariopsis felis").getNodeID(), is(second.getNodeID()));
    }

    @Test
    public void firstGenusThenSpecies() throws NodeFactoryException {
        TaxonNode first = taxonService.getOrCreateTaxon("Ariopsis", null, null);
        TaxonNode second = taxonService.getOrCreateTaxon("Ariopsis felis", null, null);
        assertThat(first.getExternalId(), not(is(second.getExternalId())));
        assertThat(first.getNodeID(), not(is(second.getNodeID())));
        assertThat(taxonService.findTaxonByName("Ariopsis felis").getNodeID(), is(second.getNodeID()));
        assertThat(taxonService.findTaxonByName("Ariopsis").getNodeID(), is(first.getNodeID()));
    }

    @Test
    public void noDuplicatesOnAlternateNames() throws NodeFactoryException {
        TaxonNode first = taxonService.getOrCreateTaxon("Cliona caribbaea", null, null);
        TaxonNode second = taxonService.getOrCreateTaxon("Cliona langae", null, null);
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
        TaxonNode bicolor = taxonService.getOrCreateTaxon("Mimesa bicolor foo", null, null);
        assertThat(taxonService.findTaxonByName("Mimesa bicolor"), is(notNullValue()));
        taxonService.getOrCreateTaxon("Mimesa bicolor", null, null);
        TaxonNode equestris = taxonService.getOrCreateTaxon("Mimesa equestris bla", null, null);
        assertThat(taxonService.findTaxonByName("Mimesa equestris"), is(notNullValue()));
        taxonService.getOrCreateTaxon("Mimesa equestris", null, null);
        assertThat(taxonService.findTaxonByName("Mimesa equestris bla").getNodeID(), is(equestris.getNodeID()));
        assertThat(bicolor.getNodeID(), not(Is.is(equestris.getNodeID())));
    }

    @Test
    public void specialCharacters() throws NodeFactoryException {
        taxonService.setCorrector(new TaxonNameCorrector());
        TaxonNode taxon = taxonService.getOrCreateTaxon("Longspine swimming crab", null, null);
        assertThat(taxon.getName(), is("Acheloüs spinicarpus"));
        assertThat(taxon.getExternalId(), is(notNullValue()));
        assertThat(taxon.getPath(), is(notNullValue()));

        TaxonNode secondTaxon = taxonService.getOrCreateTaxon("Portunus spinicarpus", null, null);
        assertThat(secondTaxon.getNodeID(), is(taxon.getNodeID()));
        assertThat(secondTaxon.getName(), is("Acheloüs spinicarpus"));

        assertThat(taxonService.findTaxonByName("Acheloüs spinicarpus"), is(notNullValue()));
        assertThat(taxonService.findTaxonByName("Portunus spinicarpus"), is(notNullValue()));
    }

    @Test
    public void createHomoSapiens() throws NodeFactoryException {
        TaxonNode taxon = taxonService.getOrCreateTaxon("Homo sapiens", null, null);
        assertThat(taxon.getName(), is("Homo sapiens"));
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Primates"));
        assertThat(taxon.getCommonNames(), containsString("human"));
        assertThat(taxon.getPathNames(), containsString("genus"));
        assertZeroHits(taxonService, "no:match");
    }

    @Test(expected = NodeFactoryException.class)
    public void nameTooShort() throws NodeFactoryException {
        taxonService.getOrCreateTaxon("", null, null);
    }

    @Test
    public void nameTooShortButHasExternalId() throws NodeFactoryException {
        TaxonNode taxon = taxonService.getOrCreateTaxon("", "EOL:327955", null);
        assertThat(taxon.getPath(), containsString("Homo sapiens"));
    }

    private void assertZeroHits(TaxonServiceImpl taxonService, String taxonName) {
        IndexHits<Node> hits = taxonService.findCloseMatchesForTaxonName(taxonName);
        assertThat(hits.size(), is(0));
    }
}
