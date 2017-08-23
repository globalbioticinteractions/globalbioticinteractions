package org.eol.globi.service;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.data.TaxonIndex;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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

    private static PropertyEnricher taxonEnricher;

    @BeforeClass
    public static void init() {
        taxonEnricher = PropertyEnricherFactory.createTaxonEnricher();
    }

    @AfterClass
    public static void shutdown() {
        taxonEnricher.shutdown();
    }

    @Override
    protected TaxonIndex getOrCreateTaxonIndex() {
        return super.getOrCreateTaxonIndex(taxonEnricher);
    }

    @Test
    public void enrichTwoTaxons() throws NodeFactoryException, IOException {
        final TaxonImpl blabla = new TaxonImpl("Homo sapiens", "blabla");
        blabla.setPath(null);
        Taxon taxon = taxonIndex.getOrCreateTaxon(blabla);
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Animalia"));
        assertThat(taxon.getRank(), containsString("Species"));

        taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Homo sapiens", null));
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Animalia"));


        taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Ariopsis felis", null));
        assertThat(taxon.getExternalId(), is("EOL:223038"));
        assertThat(taxon.getPath(), containsString("Animalia"));

        Taxon sameTaxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Ariopsis felis", null));
        assertThat(((NodeBacked)taxon).getNodeID(), is(((NodeBacked)sameTaxon).getNodeID()));

        taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Pitar fulminatus", null));
        assertThat(taxon.getExternalId(), is("EOL:448962"));
        assertThat(taxon.getPath(), is(not(PropertyAndValueDictionary.NO_MATCH)));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/12
    public void foraminifera() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Foraminifera", null));
        assertThat(taxon.getExternalId(), is("EOL:4888"));
        assertThat(taxon.getName(), is("Foraminifera"));
        assertThat(taxon.getPath(), containsString(CharsetConstant.SEPARATOR + "Foraminifera"));
    }


    @Test
    public void unacceptedWoRMSSpecies() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Sterrhurus concavovesiculus", null));
        assertUnacceptedWoRMS(taxon);
    }

    @Test
    public void someTest() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Aedes furcifer", "EOL:754947"));
        assertThat(taxon.getPathIds(), is("WORMS:1 | WORMS:2 | WORMS:793 | WORMS:19948 | WORMS:108400 | WORMS:108402 | WORMS:468918 | WORMS:108418 | WORMS:108471 | WORMS:724982 | WORMS:108758 | WORMS:726834"));

    }

    @Test
    public void barleyMosaicVirus() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Barley mosaic virus (Japan)", null));
        assertThat(taxon.getPath(), is(nullValue()));
    }

    @Test
    public void alfalfaMosaicVirus() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Alfalfa mosaic virus", null));
        assertThat(taxon.getPath(), containsString("Alfalfa mosaic virus"));
    }

    @Test
    public void zikaVirus() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Zika virus (ZIKV)", "EOL:541190"));
        assertThat(taxon.getPath(), containsString("Flaviviridae"));
        assertThat(TaxonUtil.isResolved(taxon), is(true));
    }

    @Test
    public void resolveAcceptedNameStartingFromUnacceptedITISTSN() throws IOException, NodeFactoryException {
        // related to issue https://github.com/jhpoelen/eol-globi-data/issues/110
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl(null, "ITIS:167353"));
        assertThat(taxon.getExternalId(), is("ITIS:692068"));
        assertThat(taxon.getName(), is("Scorpaenichthys marmoratus"));
    }

    protected void assertUnacceptedWoRMS(Taxon taxon) {
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
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("donald duckus", "WORMS:729172"));
        assertUnacceptedWoRMS(taxon);
    }

    @Test
    public void iNaturalistTaxon() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Celtis laevigata", "INAT_TAXON:81792"));
        assertThat(taxon.getExternalId(), is("INAT_TAXON:81792"));
        assertThat(taxon.getName(), is("Celtis laevigata"));
        assertThat(taxon.getPathIds(), is("INAT_TAXON:47126 | INAT_TAXON:211194 | INAT_TAXON:47125 | INAT_TAXON:47124 | INAT_TAXON:47132 | INAT_TAXON:53781 | INAT_TAXON:54858 | INAT_TAXON:81792"));
        assertThat(taxon.getPath(), containsString(CharsetConstant.SEPARATOR + "Celtis"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getPathNames(), containsString(CharsetConstant.SEPARATOR + "species"));
    }

    @Test
    public void iNaturalistTaxon2() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Donaldus duckus", "INAT_TAXON:58831"));
        assertThat(taxon.getExternalId(), is("INAT_TAXON:58831"));
        assertThat(taxon.getName(), is("Heterotheca grandiflora"));
        assertThat(taxon.getPathIds(), containsString("INAT_TAXON:58831"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getPathNames(), containsString(CharsetConstant.SEPARATOR + "species"));
    }

    @Test
    public void emptyTaxon() throws IOException, NodeFactoryException, PropertyEnricherException {
        Taxon enrich = TaxonUtil.enrich(taxonEnricher, new TaxonImpl("", ""));
        assertThat(enrich.getName(), is(""));
        assertThat(enrich.getExternalId(), is(""));
    }


    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/59
    public void greySmoothhound() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Grey Smoothhound", null));
        assertThat(taxon.getExternalId(), is("EOL:207918"));
        assertThat(taxon.getName(), is("Mustelus californicus"));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/60
    public void gallTissue() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("gall tissue (Q. robur)", null));
        assertThat(taxon.getName(), is("Quercus robur"));
        assertThat(taxon.getExternalId(), is("EOL:1151323"));
    }

    @Test
    public void chromatomyiaScabiosae() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Chromatomyia scabiosae", null));
        assertThat(taxon.getExternalId(), is("EOL:3492979"));
        assertThat(taxon.getName(), is("Chromatomyia scabiosae"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Diptera | Agromyzidae | Chromatomyia | Chromatomyia scabiosae"));
    }


    @Test
    public void sphyrnaMokarran() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Sphyrna mokarran", null));
        assertThat(taxon.getName(), is("Sphyrna mokarran"));
        assertThat(taxon.getPath(), is("Animalia | Chordata | Elasmobranchii | Carcharhiniformes | Sphyrnidae | Sphyrna | Sphyrna mokarran"));
        assertThat(taxon.getExternalId(), is("EOL:224168"));
    }

    @Ignore("Other suspension feeders resolves to Other, which is an alternate name for  http://eol.org/pages/2913255/overview")
    @Test
    public void otherSuspensionFeeders() throws IOException, NodeFactoryException {
        Taxon taxon = taxonIndex.getOrCreateTaxon(new TaxonImpl("Other suspension feeders", null));
        assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getName(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getPath(), is(PropertyAndValueDictionary.NO_MATCH));
    }

    @Test
    public void sediment() throws IOException, NodeFactoryException {
        assertThat(taxonIndex.getOrCreateTaxon(new TaxonImpl("Sediment", null)).getExternalId(), is("ENVO:00002007"));
        assertThat(taxonIndex.getOrCreateTaxon(new TaxonImpl("sediment", null)).getExternalId(), is("ENVO:00002007"));
        assertIsOrganicMaterial(taxonIndex.getOrCreateTaxon(new TaxonImpl("detritus", null)));
        assertIsOrganicMaterial(taxonIndex.getOrCreateTaxon(new TaxonImpl("Detritus", null)));
        assertThat(taxonIndex.getOrCreateTaxon(new TaxonImpl("Detritus", null)).getExternalId(), is("ENVO:01000155"));
    }

    @Test
    public void detritusById() throws IOException, NodeFactoryException {
        Taxon someOrganicMaterial = taxonIndex.getOrCreateTaxon(new TaxonImpl("somehing", "ENVO:01000155"));
        assertThat(someOrganicMaterial.getExternalId(), is("ENVO:01000155"));
        assertThat(someOrganicMaterial.getPath(), is("environmental material | organic material"));
    }

    protected void assertIsOrganicMaterial(Taxon detritus) {
        assertThat(detritus.getExternalId(), is("ENVO:01000155"));
        assertThat(detritus.getPath(), is("environmental material | organic material"));
    }

    @Test
    public void noNameButExternalId() throws NodeFactoryException {
        Specimen specimen = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("testing123", null, null, null)), new TaxonImpl(PropertyAndValueDictionary.NO_NAME, "EOL:223038"));
        assertThat(specimen, is(notNullValue()));
        Iterable<Relationship> classifications = NodeUtil.getClassifications(specimen);
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
        Specimen specimen = nodeFactory.createSpecimen(nodeFactory.createStudy(new StudyImpl("testing123", null, null, null)), new TaxonImpl(null, "urn:lsid:biodiversity.org.au:afd.taxon:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae"));
        assertThat(specimen, is(notNullValue()));
        Iterable<Relationship> classifications = NodeUtil.getClassifications(specimen);
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
