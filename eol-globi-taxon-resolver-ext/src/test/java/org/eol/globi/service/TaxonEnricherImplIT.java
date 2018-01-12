package org.eol.globi.service;

import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class TaxonEnricherImplIT {

    private static PropertyEnricher taxonEnricher;

    @BeforeClass
    public static void init() {
        taxonEnricher = PropertyEnricherFactory.createTaxonEnricher();
    }

    @AfterClass
    public static void shutdown() {
        taxonEnricher.shutdown();
    }


    private Taxon enrich(Taxon taxon) throws PropertyEnricherException {
        return TaxonUtil.mapToTaxon(taxonEnricher.enrich(TaxonUtil.taxonToMap(taxon)));
    }

    @Test
    public void enrichTwoTaxons() throws PropertyEnricherException {
        final TaxonImpl blabla = new TaxonImpl("Homo sapiens", "blabla");
        blabla.setPath(null);
        Taxon taxon = enrich(blabla);
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Animalia"));
        assertThat(taxon.getRank(), containsString("Species"));

        taxon = enrich(new TaxonImpl("Homo sapiens", null));
        assertThat(taxon.getExternalId(), is("EOL:327955"));
        assertThat(taxon.getPath(), containsString("Animalia"));


        taxon = enrich(new TaxonImpl("Ariopsis felis", null));
        assertThat(taxon.getExternalId(), is("EOL:223038"));
        assertThat(taxon.getPath(), containsString("Animalia"));

        taxon = enrich(new TaxonImpl("Pitar fulminatus", null));
        assertThat(taxon.getExternalId(), is("EOL:448962"));
        assertThat(taxon.getPath(), is(not(PropertyAndValueDictionary.NO_MATCH)));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/12
    public void foraminifera() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Foraminifera", null));
        assertThat(taxon.getExternalId(), is("EOL:4888"));
        assertThat(taxon.getName(), is("Foraminifera"));
        assertThat(taxon.getPath(), containsString(CharsetConstant.SEPARATOR + "Foraminifera"));
    }


    @Test
    public void unacceptedWoRMSSpecies() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Sterrhurus concavovesiculus", null));
        assertUnacceptedWoRMS(taxon);
    }

    @Test
    public void someTest() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Aedes furcifer", "EOL:754947"));
        assertThat(taxon.getPathIds(), is("WORMS:1 | WORMS:2 | WORMS:793 | WORMS:19948 | WORMS:108400 | WORMS:108402 | WORMS:468918 | WORMS:108418 | WORMS:108471 | WORMS:724982 | WORMS:108758 | WORMS:726834"));

    }

    @Test
    public void barleyMosaicVirus() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Barley mosaic virus (Japan)", null));
        assertThat(taxon.getPath(), is(nullValue()));
    }

    @Test
    public void alfalfaMosaicVirus() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Alfalfa mosaic virus", null));
        assertThat(taxon.getPath(), containsString("Alfalfa mosaic virus"));
    }

    @Test
    public void zikaVirus() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Zika virus (ZIKV)", "EOL:541190"));
        assertThat(taxon.getPath(), containsString("Flaviviridae"));
        assertThat(TaxonUtil.isResolved(taxon), is(true));
    }

    @Test
    public void resolveAcceptedNameStartingFromUnacceptedITISTSN() throws PropertyEnricherException {
        // related to issue https://github.com/jhpoelen/eol-globi-data/issues/110
        Taxon taxon = enrich(new TaxonImpl(null, "ITIS:167353"));
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
    public void unacceptedWoRMSSpeciesById() throws PropertyEnricherException {
        // note that the ID takes precedence over the name
        Taxon taxon = enrich(new TaxonImpl("donald duckus", "WORMS:729172"));
        assertUnacceptedWoRMS(taxon);
    }

    @Test
    public void iNaturalistTaxon() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Celtis laevigata", "INAT_TAXON:81792"));
        assertThat(taxon.getExternalId(), is("INAT_TAXON:81792"));
        assertThat(taxon.getName(), is("Celtis laevigata"));
        assertThat(taxon.getPathIds(), is("INAT_TAXON:47126 | INAT_TAXON:211194 | INAT_TAXON:47125 | INAT_TAXON:47124 | INAT_TAXON:47132 | INAT_TAXON:53781 | INAT_TAXON:54858 | INAT_TAXON:81792"));
        assertThat(taxon.getPath(), containsString(CharsetConstant.SEPARATOR + "Celtis"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getPathNames(), containsString(CharsetConstant.SEPARATOR + "species"));
    }

    @Test
    public void iNaturalistTaxon2() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Donaldus duckus", "INAT_TAXON:58831"));
        assertThat(taxon.getExternalId(), is("INAT_TAXON:58831"));
        assertThat(taxon.getName(), is("Heterotheca grandiflora"));
        assertThat(taxon.getPathIds(), containsString("INAT_TAXON:58831"));
        assertThat(taxon.getRank(), is("species"));
        assertThat(taxon.getPathNames(), containsString(CharsetConstant.SEPARATOR + "species"));
    }

    @Test
    public void emptyTaxon() throws PropertyEnricherException, PropertyEnricherException {
        Taxon enrich = TaxonUtil.enrich(taxonEnricher, new TaxonImpl("", ""));
        assertThat(enrich.getName(), is(""));
        assertThat(enrich.getExternalId(), is(""));
    }


    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/59
    public void greySmoothhound() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Grey Smoothhound", null));
        assertThat(taxon.getExternalId(), is("EOL:207918"));
        assertThat(taxon.getName(), is("Mustelus californicus"));
    }

    @Test
    // see https://github.com/jhpoelen/eol-globi-data/issues/60
    public void gallTissue() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("gall tissue (Q. robur)", null));
        assertThat(taxon.getName(), is("Quercus robur"));
        assertThat(taxon.getExternalId(), is("EOL:1151323"));
    }

    @Test
    public void chromatomyiaScabiosae() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Chromatomyia scabiosae", null));
        assertThat(taxon.getExternalId(), is("EOL:3492979"));
        assertThat(taxon.getName(), is("Chromatomyia scabiosae"));
        assertThat(taxon.getPath(), is("Animalia | Arthropoda | Insecta | Diptera | Agromyzidae | Chromatomyia | Chromatomyia scabiosae"));
    }


    @Test
    public void sphyrnaMokarran() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Sphyrna mokarran", null));
        assertThat(taxon.getName(), is("Sphyrna mokarran"));
        assertThat(taxon.getPath(), is("Animalia | Chordata | Elasmobranchii | Carcharhiniformes | Sphyrnidae | Sphyrna | Sphyrna mokarran"));
        assertThat(taxon.getExternalId(), is("EOL:224168"));
    }

    @Ignore("Other suspension feeders resolves to Other, which is an alternate name for  http://eol.org/pages/2913255/overview")
    @Test
    public void otherSuspensionFeeders() throws PropertyEnricherException {
        Taxon taxon = enrich(new TaxonImpl("Other suspension feeders", null));
        assertThat(taxon.getExternalId(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getName(), is(PropertyAndValueDictionary.NO_MATCH));
        assertThat(taxon.getPath(), is(PropertyAndValueDictionary.NO_MATCH));
    }

    @Test
    public void sediment() throws PropertyEnricherException {
        assertThat(enrich(new TaxonImpl("Sediment", null)).getExternalId(), is("ENVO:00002007"));
        assertThat(enrich(new TaxonImpl("sediment", null)).getExternalId(), is("ENVO:00002007"));
        assertIsOrganicMaterial(enrich(new TaxonImpl("detritus", null)));
        assertIsOrganicMaterial(enrich(new TaxonImpl("Detritus", null)));
        assertThat(enrich(new TaxonImpl("Detritus", null)).getExternalId(), is("ENVO:01000155"));
    }

    @Test
    public void detritusById() throws PropertyEnricherException {
        Taxon someOrganicMaterial = enrich(new TaxonImpl("somehing", "ENVO:01000155"));
        assertThat(someOrganicMaterial.getExternalId(), is("ENVO:01000155"));
        assertThat(someOrganicMaterial.getPath(), is("environmental material | organic material"));
    }

    protected void assertIsOrganicMaterial(Taxon detritus) {
        assertThat(detritus.getExternalId(), is("ENVO:01000155"));
        assertThat(detritus.getPath(), is("environmental material | organic material"));
    }

    @Test
    public void noNameButExternalId() throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl(PropertyAndValueDictionary.NO_NAME, "EOL:223038");
        Taxon taxonNode = enrich(taxon);

        assertThat(taxonNode.getExternalId(), is("EOL:223038"));
        assertThat(taxonNode.getName(), is("Ariopsis felis"));
        assertThat(taxonNode.getPath(), is("Animalia | Chordata | Actinopterygii | Siluriformes | Ariidae | Ariopsis | Ariopsis felis"));
    }

    @Test
    public void atlasOfLivingAustralia() throws PropertyEnricherException {
        TaxonImpl taxon = new TaxonImpl(null, "urn:lsid:biodiversity.org.au:afd.taxon:31a9b8b8-4e8f-4343-a15f-2ed24e0bf1ae");
        Taxon taxonNode = enrich(taxon);
        assertThat(taxonNode.getExternalId(), is("AFD:e6aff6af-ff36-4ad5-95f2-2dfdcca8caff"));
        assertThat(taxonNode.getName(), is("Osphranter rufus"));
    }


}
