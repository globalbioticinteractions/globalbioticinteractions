package org.eol.globi.service;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class TaxonEnricherImplIT extends GraphDBTestCase {

    @Override
    protected TaxonIndex getOrCreateTaxonIndex() {
        return super.getOrCreateTaxonIndex(PropertyEnricherFactory.createTaxonEnricher());
    }

    @Test
    public void enrichTwoTaxons() throws NodeFactoryException, IOException {
        final TaxonImpl blabla = new TaxonImpl("Homo sapiens", "blabla");
        blabla.setPath(null);
        TaxonNode taxon = taxonIndex.getOrCreateTaxon(blabla);
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Animalia"));
        assertThat(taxon.getRank(), containsString("Species"));

        taxon = taxonIndex.getOrCreateTaxon("Homo sapiens");
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Animalia"));


        taxon = taxonIndex.getOrCreateTaxon("Ariopsis felis");
        assertThat(taxon.getExternalId(), is("EOL:223038"));
        assertThat(taxon.getPath(), containsString("Animalia"));

        TaxonNode sameTaxon = taxonIndex.getOrCreateTaxon("Ariopsis felis");
        assertThat(taxon.getNodeID(), is(sameTaxon.getNodeID()));

        taxon = taxonIndex.getOrCreateTaxon("Pitar fulminatus");
        assertThat(taxon.getExternalId(), is("EOL:448962"));
        assertThat(taxon.getPath(), is(not(PropertyAndValueDictionary.NO_MATCH)));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/12
    public void foraminifera() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("Foraminifera");
        assertThat(taxon.getExternalId(), is("EOL:4888"));
        assertThat(taxon.getName(), is("Foraminifera"));
        assertThat(taxon.getPath(), containsString(CharsetConstant.SEPARATOR + "Foraminifera"));
    }


    @Test
    public void unacceptedWoRMSSpecies() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("Sterrhurus concavovesiculus");
        assertUnacceptedWoRMS(taxon);
    }

    @Test
    public void someTest() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("Aedes furcifer", "EOL:754947");
        assertThat(taxon.getPathIds(), is("WORMS:1 | WORMS:2 | WORMS:793 | WORMS:19948 | WORMS:108400 | WORMS:108402 | WORMS:468918 | WORMS:108418 | WORMS:108471 | WORMS:724982 | WORMS:108758 | WORMS:726834"));

    }

    @Test
    public void barleyMosaicVirus() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("Barley mosaic virus (Japan)");
        assertThat(taxon.getPath(), is(nullValue()));
    }

    @Test
    public void alfalfaMosaicVirus() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("Alfalfa mosaic virus");
        assertThat(taxon.getPath(), containsString("Alfalfa mosaic virus"));
    }

    @Test
    public void zikaVirus() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("Zika virus (ZIKV)", "EOL:541190");
        assertThat(taxon.getPath(), containsString("Flaviviridae"));
        assertThat(TaxonUtil.isResolved(taxon), is(true));
    }

    @Test
    public void resolveAcceptedNameStartingFromUnacceptedITISTSN() throws IOException, NodeFactoryException {
        // related to issue https://github.com/jhpoelen/eol-globi-data/issues/110
        TaxonNode taxon = taxonIndex.getOrCreateTaxon(null, "ITIS:167353", null);
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
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("donald duckus", "WORMS:729172", null);
        assertUnacceptedWoRMS(taxon);
    }

    @Test
    public void inaturalistTaxon() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("Celtis laevigata", "INAT_TAXON:81792", null);
        assertThat(taxon.getExternalId(), is("INAT_TAXON:81792"));
        assertThat(taxon.getName(), is("Celtis laevigata"));
        assertThat(taxon.getPathIds(), is("INAT_TAXON:47126 | INAT_TAXON:211194 | INAT_TAXON:47125 | INAT_TAXON:47124 | INAT_TAXON:47132 | INAT_TAXON:53781 | INAT_TAXON:54858 | INAT_TAXON:81792"));
        assertThat(taxon.getPath(), containsString(CharsetConstant.SEPARATOR + "Celtis"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getPathNames(), containsString(CharsetConstant.SEPARATOR + "species"));
    }

    @Test
    public void emptyTaxon() throws IOException, NodeFactoryException, PropertyEnricherException {
        Taxon enrich = TaxonUtil.enrich(PropertyEnricherFactory.createTaxonEnricher(), new TaxonImpl("", ""));
        assertThat(enrich.getName(), is(""));
        assertThat(enrich.getExternalId(), is(""));
    }


    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/59
    public void greySmoothhound() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("Grey Smoothhound");
        assertThat(taxon.getExternalId(), is("EOL:207918"));
        assertThat(taxon.getName(), is("Mustelus californicus"));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/60
    public void gallTissue() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("gall tissue (Q. robur)");
        assertThat(taxon.getName(), is("Quercus robur"));
        assertThat(taxon.getExternalId(), is("EOL:1151323"));
    }

    @Test
    public void chromatomyiaScabiosae() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("Chromatomyia scabiosae");
        assertThat(taxon.getExternalId(), is("EOL:3492979"));
        assertThat(taxon.getName(), is("Chromatomyia scabiosae"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Diptera | Agromyzidae | Chromatomyia | Chromatomyia scabiosae"));
    }


    @Test
    public void sphyrnaMokarran() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("Sphyrna mokarran");
        assertThat(taxon.getName(), is("Sphyrna mokarran"));
        assertThat(taxon.getPath(), is("Animalia | Chordata | Elasmobranchii | Carcharhiniformes | Sphyrnidae | Sphyrna | Sphyrna mokarran"));
        assertThat(taxon.getExternalId(), is("EOL:224168"));
    }

    @Ignore("Other suspension feeders resolves to Other, which is an alternate name for  http://eol.org/pages/2913255/overview")
    @Test
    public void otherSuspensionFeeders() throws IOException, NodeFactoryException {
        TaxonNode taxon = taxonIndex.getOrCreateTaxon("Other suspension feeders");
        assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getName(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getPath(), is(PropertyAndValueDictionary.NO_MATCH));
    }

    @Test
    public void sediment() throws IOException, NodeFactoryException {
        assertThat(taxonIndex.getOrCreateTaxon("Sediment").getExternalId(), is("ENVO:00002007"));
        assertThat(taxonIndex.getOrCreateTaxon("sediment").getExternalId(), is("ENVO:00002007"));
        assertIsOrganicMaterial(taxonIndex.getOrCreateTaxon("detritus"));
        assertIsOrganicMaterial(taxonIndex.getOrCreateTaxon("Detritus"));
        assertThat(taxonIndex.getOrCreateTaxon("Detritus").getExternalId(), is("ENVO:01000155"));
    }

    @Test
    public void detritusById() throws IOException, NodeFactoryException {
        TaxonNode someOrganicMaterial = taxonIndex.getOrCreateTaxon("somehing", "ENVO:01000155", null);
        assertThat(someOrganicMaterial.getExternalId(), is("ENVO:01000155"));
        assertThat(someOrganicMaterial.getPath(), is("environmental material | organic material"));
    }

    protected void assertIsOrganicMaterial(TaxonNode detritus) {
        assertThat(detritus.getExternalId(), is("ENVO:01000155"));
        assertThat(detritus.getPath(), is("environmental material | organic material"));
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
