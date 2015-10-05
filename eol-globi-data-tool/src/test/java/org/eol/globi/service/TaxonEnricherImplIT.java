package org.eol.globi.service;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryImpl;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.taxon.TaxonNameCorrector;
import org.eol.globi.taxon.TaxonIndexImpl;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class TaxonEnricherImplIT extends GraphDBTestCase {

    private PropertyEnricher enricher;

    @Before
    public void start() {
        enricher = PropertyEnricherFactory.createTaxonEnricher();
        nodeFactory = new NodeFactoryImpl(getGraphDb(), new TaxonIndexImpl(enricher, new TaxonNameCorrector(), getGraphDb()));
    }

    @Test
    public void enrichTwoTaxons() throws NodeFactoryException, IOException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Homo sapiens", "blabla", null);
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Animalia"));
        assertThat(taxon.getRank(), containsString("Species"));

        taxon = nodeFactory.getOrCreateTaxon("Homo sapiens");
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Animalia"));


        taxon = nodeFactory.getOrCreateTaxon("Ariopsis felis");
        assertThat(taxon.getExternalId(), is("EOL:223038"));
        assertThat(taxon.getPath(), containsString("Animalia"));

        TaxonNode sameTaxon = nodeFactory.getOrCreateTaxon("Ariopsis felis");
        assertThat(taxon.getNodeID(), is(sameTaxon.getNodeID()));

        taxon = nodeFactory.getOrCreateTaxon("Pitar fulminatus");
        assertThat(taxon.getExternalId(), is("EOL:448962"));
        assertThat(taxon.getPath(), is(not(PropertyAndValueDictionary.NO_MATCH)));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/12
    public void foraminifera() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Foraminifera");
        assertThat(taxon.getExternalId(), is("EOL:4888"));
        assertThat(taxon.getName(), is("Foraminifera"));
        assertThat(taxon.getPath(), containsString(CharsetConstant.SEPARATOR + "Foraminifera"));
    }



    @Test
    public void unacceptedWoRMSSpecies() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Sterrhurus concavovesiculus");
        assertUnacceptedWoRMS(taxon);
    }

    @Test
    public void resolveAcceptedNameStartingFromUnacceptedITISTSN() throws IOException, NodeFactoryException {
        // related to issue https://github.com/jhpoelen/eol-globi-data/issues/110
        TaxonNode taxon = nodeFactory.getOrCreateTaxon(null, "ITIS:167353", null);
        assertThat(taxon.getExternalId(), is("ITIS:692068"));
        assertThat(taxon.getName(), is("Scorpaenichthys marmoratus"));
    }

    protected void assertUnacceptedWoRMS(TaxonNode taxon) {
        assertThat(taxon.getExternalId(), is("WORMS:726834"));
        assertThat(taxon.getName(), is("Lecithochirium concavovesiculus"));
        assertThat(taxon.getPathIds(), is("WORMS:1 | WORMS:2 | WORMS:793 | WORMS:19948 | WORMS:108400 | WORMS:108402 | WORMS:468918 | WORMS:108418 | WORMS:108471 | WORMS:724982 | WORMS:108758 | WORMS:726834"));
        assertThat(taxon.getPath(), containsString(CharsetConstant.SEPARATOR + "Platyhelminthes"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getPathNames(), containsString(CharsetConstant.SEPARATOR + "species"));
    }

    @Test
    public void unacceptedWoRMSSpeciesById() throws IOException, NodeFactoryException {
        // note that the ID takes precedence over the name
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("donald duckus", "WORMS:729172", null);
        assertUnacceptedWoRMS(taxon);
    }

    @Test
    public void emptyTaxon() throws IOException, NodeFactoryException, PropertyEnricherException {
        Taxon enrich = TaxonUtil.enrich(enricher, new TaxonImpl("", ""));
        assertThat(enrich.getName(), is(""));
        assertThat(enrich.getExternalId(), is(""));
    }


    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/59
    public void greySmoothhound() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Grey Smoothhound");
        assertThat(taxon.getExternalId(), is("EOL:207918"));
        assertThat(taxon.getName(), is("Mustelus californicus"));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/60
    public void gallTissue() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("gall tissue (Q. robur)");
        assertThat(taxon.getName(), is("Quercus robur"));
        assertThat(taxon.getExternalId(), is("EOL:1151323"));
    }

    @Test
    public void chromatomyiaScabiosae() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Chromatomyia scabiosae");
        assertThat(taxon.getExternalId(), is("EOL:3492979"));
        assertThat(taxon.getName(), is("Chromatomyia scabiosae"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Diptera | Agromyzidae | Chromatomyia | Chromatomyia scabiosae"));
    }


    @Test
    public void sphyrnaMokarran() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Sphyrna mokarran");
        assertThat(taxon.getName(), is("Sphyrna mokarran"));
        assertThat(taxon.getPath(), is("Animalia | Chordata | Elasmobranchii | Carcharhiniformes | Sphyrnidae | Sphyrna | Sphyrna mokarran"));
        assertThat(taxon.getExternalId(), is("EOL:224168"));
    }

    @Ignore("Other suspension feeders resolves to Other, which is an alternate name for  http://eol.org/pages/2913255/overview")
    @Test
    public void otherSuspensionFeeders() throws IOException, NodeFactoryException {
        TaxonNode taxon = nodeFactory.getOrCreateTaxon("Other suspension feeders");
        assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getName(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getPath(), is(PropertyAndValueDictionary.NO_MATCH));
    }

    @Test
    public void sediment() throws IOException, NodeFactoryException {
        assertThat(nodeFactory.getOrCreateTaxon("Sediment").getExternalId(), is("ENVO:00002007"));
        assertThat(nodeFactory.getOrCreateTaxon("sediment").getExternalId(), is("ENVO:00002007"));
        assertThat(nodeFactory.getOrCreateTaxon("detritus").getExternalId(), is("ENVO:01000155"));
        assertThat(nodeFactory.getOrCreateTaxon("Detritus").getExternalId(), is("ENVO:01000155"));
    }

    @Test
    public void noNameButExternalId() throws NodeFactoryException {
        Specimen specimen = nodeFactory.createSpecimen(nodeFactory.createStudy("testing123"), PropertyAndValueDictionary.NO_NAME, "EOL:223038");
        assertThat(specimen, is(notNullValue()));
        Iterable<Relationship> classifications = specimen.getClassifications();
        int count = 0;
        for (Relationship classification : classifications) {
            TaxonNode taxonNode = new TaxonNode(classification.getEndNode());
            assertThat(taxonNode.getExternalId(), is("EOL:223038"));
            assertThat(taxonNode.getName(), is("Ariopsis felis"));
            assertThat(taxonNode.getPath(), is("Animalia | Chordata | Actinopterygii | Siluriformes | Ariidae | Ariopsis | Ariopsis felis"));
            count++;
        }
        assertThat(count, is(1));
    }

    @Test
    public void atlasOfLivingAustralia() throws NodeFactoryException {
        Specimen specimen = nodeFactory.createSpecimen(nodeFactory.createStudy("testing123"), null, "urn:lsid:biodiversity.org.au:afd.taxon:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae");
        assertThat(specimen, is(notNullValue()));
        Iterable<Relationship> classifications = specimen.getClassifications();
        int count = 0;
        for (Relationship classification : classifications) {
            TaxonNode taxonNode = new TaxonNode(classification.getEndNode());
            assertThat(taxonNode.getExternalId(), is("AFD:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae"));
            assertThat(taxonNode.getName(), is("Macropus rufus"));
            count++;
        }
        assertThat(count, is(1));
    }


}
