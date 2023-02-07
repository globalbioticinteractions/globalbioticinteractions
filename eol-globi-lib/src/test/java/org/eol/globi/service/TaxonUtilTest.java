package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaxonUtilTest {

    @Test
    public void homonym() {
        TaxonImpl taxon = new TaxonImpl();
        taxon.setName("Lestes");
        taxon.setExternalId("some:id");
        taxon.setPath("Insecta|Lestidae|Lestes");
        taxon.setPathNames("class|family|genus");

        TaxonImpl otherTaxon = new TaxonImpl();
        otherTaxon.setName("Lestes");
        otherTaxon.setExternalId("some:otherid");
        otherTaxon.setPath("Mammalia|Mesonychidae|Lestes");
        otherTaxon.setPathNames("class|family|genus");

        assertTrue(TaxonUtil.likelyHomonym(taxon, otherTaxon));
    }

    @Test
    public void resolveSubclassName() {
        Map<String, String> properties = new TreeMap<String, String>() {{
            put("sourceTaxonClassName", "Arachnida");
            put("sourceTaxonSubclassName", "Acari");
        }};

        String name = TaxonUtil.generateSourceTaxonName(properties);
        assertThat(name, is("Acari"));

    }

    @Test
    public void resolveSubclassName2() {
        Map<String, String> properties = new TreeMap<String, String>() {{
            put("sourceTaxonClassName", "Arachnida");
            put("sourceTaxonSubclass", "Acari");
        }};

        String name = TaxonUtil.generateSourceTaxonName(properties);
        assertThat(name, is("Acari"));

    }

    @Test
    public void resolveSubclassName3() {
        Map<String, String> properties = new TreeMap<String, String>() {{
            put("sourceTaxonClass", "Arachnida");
            put("sourceTaxonSubclassName", "Acari");
        }};

        String name = TaxonUtil.generateSourceTaxonName(properties);
        assertThat(name, is("Acari"));

    }
    @Test
    public void generateSourceTaxonPath2() {
        Map<String, String> properties = new TreeMap<String, String>() {{
            put("sourceTaxonClass", "Arachnida");
            put("sourceTaxonSubclassName", "Acari");
        }};

        String name = TaxonUtil.generateSourceTaxonPath(properties);
        assertThat(name, is("Arachnida | Acari"));

    }

    @Test
    public void generateSourceTaxonPath3() {
        Map<String, String> properties = new TreeMap<String, String>() {{
            put("sourceTaxonClassName", "Arachnida");
            put("sourceTaxonSubclass", "Acari");
        }};

        String name = TaxonUtil.generateSourceTaxonPath(properties);
        assertThat(name, is("Arachnida | Acari"));

    }

    @Test
    public void anotherHomonym() {
        TaxonImpl taxon = new TaxonImpl();

        taxon.setName("Hymenolepis");
        taxon.setExternalId("some:id");
        taxon.setPath("Plantae | Tracheophyta | Magnoliopsida | Asterales | Asteraceae | Hymenolepis");
        taxon.setPathNames("kingdom | phylum | class | order | family | genus");

        TaxonImpl otherTaxon = new TaxonImpl();
        otherTaxon.setName("Hymenolepis");
        otherTaxon.setExternalId("some:otherid");
        otherTaxon.setPath("Animalia | Platyhelminthes | Cestoda | Cyclophyllidea | Hymenolepididae | Hymenolepis");
        otherTaxon.setPathNames("kingdom | phylum | class | order | family | genus");

        assertTrue(TaxonUtil.likelyHomonym(taxon, otherTaxon));
    }

    @Test
    public void homonymVenturia() {
        TaxonImpl taxon = new TaxonImpl();
        taxon.setName("Venturia");
        taxon.setExternalId("some:id");
        taxon.setPath("Ichneumonidae|Venturia");
        taxon.setPathNames("family|genus");

        TaxonImpl otherTaxon = new TaxonImpl();
        otherTaxon.setName("Venturia");
        otherTaxon.setExternalId("some:otherid");
        otherTaxon.setPath("Fungi | Ascomycota | Dothideomycetes | Venturiales | Venturiaceae | Venturia");
        otherTaxon.setPathNames("kingdom | phylum | class | order | family | genus");

        assertTrue(TaxonUtil.likelyHomonym(taxon, otherTaxon));
    }

    @Test
    public void noHomonymNotEnoughPathInfo() {
        TaxonImpl taxon = new TaxonImpl();
        taxon.setName("Lestes");
        taxon.setPath("x|y|z");
        taxon.setPathNames("a|b|c");

        TaxonImpl otherTaxon = new TaxonImpl();
        otherTaxon.setName("Lestes");
        otherTaxon.setPath("Mammalia|Mesonychidae|Lestes");
        otherTaxon.setPathNames("class|family|genus");

        assertFalse(TaxonUtil.likelyHomonym(taxon, otherTaxon));
    }

    @Test
    public void homonymNull() {
        TaxonImpl otherTaxon = new TaxonImpl();
        otherTaxon.setName("Lestes");
        otherTaxon.setPath("Mammalia|Mesonychidae|Lestes");
        otherTaxon.setPathNames("class|family|genus");

        assertFalse(TaxonUtil.likelyHomonym(null, otherTaxon));
        assertFalse(TaxonUtil.likelyHomonym(otherTaxon, null));
    }

    @Test
    public void homonymBacteria() {
        TaxonImpl taxon = new TaxonImpl();

        taxon.setName("Bacteria");
        taxon.setExternalId("some:id");
        taxon.setPath(" | Eukaryota | Opisthokonta | Metazoa | Eumetazoa | Bilateria | Protostomia | Ecdysozoa | Panarthropoda | Arthropoda | Mandibulata | Pancrustacea | Hexapoda | Insecta | Dicondylia | Pterygota | Neoptera | Orthopteroidea | Phasmatodea | Verophasmatodea | Anareolatae | Diapheromeridae | Diapheromerinae | Diapheromerini | Bacteria");
        taxon.setPathNames(" | superkingdom |  | kingdom |  |  |  |  |  | phylum |  |  | superclass | class |  |  | subclass | infraclass | order | suborder | infraorder | family | subfamily | tribe | genus");

        TaxonImpl otherTaxon = new TaxonImpl();
        otherTaxon.setName("Bacteria");
        otherTaxon.setExternalId("some:otherid");
        otherTaxon.setPath("Bacteria");
        otherTaxon.setPathNames("kingdom");

        assertThat(TaxonUtil.likelyHomonym(taxon, otherTaxon), is(true));
    }

    @Test
    public void copyTaxon() {
        Taxon src = new TaxonImpl("name", "id");
        src.setAuthorship("author, 2021");
        src.setStatus(new TermImpl("statusId", "statusLabel"));
        Taxon target = new TaxonImpl();
        TaxonUtil.copy(src, target);
        assertThat(target.getStatus().getId(), is("statusId"));
        assertThat(target.getStatus().getName(), is("statusLabel"));
        assertThat(target.getAuthorship(), is("author, 2021"));
    }

    @Test
    public void copyTaxonPrefillExternalURL() {
        Taxon src = new TaxonImpl("name", "GBIF:123");
        src.setStatus(new TermImpl("statusId", "statusLabel"));
        Taxon target = new TaxonImpl();
        TaxonUtil.copy(src, target);
        assertThat(target.getStatus().getId(), is("statusId"));
        assertThat(target.getStatus().getName(), is("statusLabel"));
        assertThat(target.getExternalUrl(), is("http://www.gbif.org/species/123"));
    }

    @Test
    public void notHomonym() {
        TaxonImpl taxon = new TaxonImpl();
        taxon.setName("Lestes");
        taxon.setPath("Insecta|Lestidae|Lestes");
        taxon.setPathNames("class|family|genus");

        TaxonImpl otherTaxon = new TaxonImpl();
        otherTaxon.setName("Lestes");
        otherTaxon.setPath("Insecta|Lestidae|Lestes");
        otherTaxon.setPathNames("class|family|genus");

        assertThat(TaxonUtil.likelyHomonym(taxon, otherTaxon), is(false));
    }

    @Test
    //see https://github.com/globalbioticinteractions/globalbioticinteractions/issues/249
    public void notHomonymSparseHigherOrderRanks() {
        TaxonImpl taxon = new TaxonImpl();
        taxon.setName("Medicago sativa");
        taxon.setPath("Biota | Plantae | Tracheophyta | Magnoliopsida | Fabales | Fabaceae | Medicago | Medicago sativa");
        taxon.setPathNames("Unranked|Kingdom|Phylum|Class|Order|Family|Genus|Species");

        TaxonImpl otherTaxon = new TaxonImpl();
        otherTaxon.setName("Medicago sativa");
        otherTaxon.setPath("Eukaryota|Streptophyta|Medicago|Medicago sativa|Papilionoideae|Trifolieae||Fabaceae|Viridiplantae|Fabales");
        otherTaxon.setPathNames("superkingdom|phylum|genus|species|subfamily|tribe|subclass|family|kingdom|order");
        assertFalse(TaxonUtil.likelyHomonym(taxon, otherTaxon));
    }

    @Test
    public void toTaxonImage() {
        TaxonImage image = new TaxonImage();
        image.setCommonName("foo @en | bar @de");

        Taxon taxon = new TaxonImpl("Donald duckus", "EOL:123");
        taxon.setCommonNames("bla @en | boo @de");
        taxon.setPath("one | two | three");
        Map<String, String> taxonMap = new TreeMap<String, String>(TaxonUtil.taxonToMap(taxon));
        taxonMap.put(PropertyAndValueDictionary.THUMBNAIL_URL, "http://foo/bar/thumb");
        taxonMap.put(PropertyAndValueDictionary.EXTERNAL_URL, "http://foo/bar");
        TaxonImage enrichedImage = TaxonUtil.enrichTaxonImageWithTaxon(taxonMap, image);

        assertThat(enrichedImage.getCommonName(), is("foo"));
        assertThat(enrichedImage.getTaxonPath(), is("one | two | three"));
        assertThat(enrichedImage.getInfoURL(), is("http://foo/bar"));
        assertThat(enrichedImage.getThumbnailURL(), is("http://foo/bar/thumb"));
        assertThat(enrichedImage.getPageId(), is("123"));
        assertThat(enrichedImage.getImageURL(), is(nullValue()));
    }

    @Test
    public void nullTaxonToMap() {
        Map<String, String> taxonMap = TaxonUtil.taxonToMap(null);
        assertThat(taxonMap.isEmpty(), is(true));
    }

    @Test
    public void toTaxonImageWithPreferredLanguage() {
        assertTaxonImageEnrichment("de", "boo");
    }

    public void assertTaxonImageEnrichment(String preferredLanguage, String expectedCommonName) {
        TaxonImage image = new TaxonImage();

        Taxon taxon = new TaxonImpl("Donald duckus", "EOL:123");
        taxon.setCommonNames("bla @en | boo @de");
        taxon.setPath("one | two | three");
        Map<String, String> taxonMap = new TreeMap<String, String>(TaxonUtil.taxonToMap(taxon));
        taxonMap.put(PropertyAndValueDictionary.THUMBNAIL_URL, "http://foo/bar/thumb");
        taxonMap.put(PropertyAndValueDictionary.EXTERNAL_URL, "http://foo/bar");
        TaxonImage enrichedImage = TaxonUtil.enrichTaxonImageWithTaxon(taxonMap, image, preferredLanguage);

        assertThat(enrichedImage.getCommonName(), is(expectedCommonName));
        assertThat(enrichedImage.getTaxonPath(), is("one | two | three"));
        assertThat(enrichedImage.getInfoURL(), is("http://foo/bar"));
        assertThat(enrichedImage.getThumbnailURL(), is("http://foo/bar/thumb"));
        assertThat(enrichedImage.getPageId(), is("123"));
        assertThat(enrichedImage.getImageURL(), is(nullValue()));
    }

    @Test
    public void toTaxonImageWithMissingPreferredLanguage() {
        assertTaxonImageEnrichment("jp", null);
    }

    @Test
    public void toTaxonImageIgnoreEOL() {
        // related to https://github.com/globalbioticinteractions/globalbioticinteractions/issues/382
        TaxonImage image = new TaxonImage();

        Taxon taxon = new TaxonImpl("Donald duckus", "EOL:123");
        taxon.setThumbnailUrl("something-media.eol.org");
        Map<String, String> taxonMap = new TreeMap<>(TaxonUtil.taxonToMap(taxon));
        TaxonImage enrichedImage = TaxonUtil.enrichTaxonImageWithTaxon(taxonMap, image);

        assertThat(enrichedImage.getThumbnailURL(), is(nullValue()));
    }

    @Test
    public void toTaxonImageIgnoreEOL2() {
        // related to https://github.com/globalbioticinteractions/globalbioticinteractions/issues/382
        TaxonImage image = new TaxonImage();

        Taxon taxon = new TaxonImpl("Donald duckus", "EOL:123");
        taxon.setThumbnailUrl("https://example.org");
        Map<String, String> taxonMap = new TreeMap<>(TaxonUtil.taxonToMap(taxon));
        TaxonImage enrichedImage = TaxonUtil.enrichTaxonImageWithTaxon(taxonMap, image);

        assertThat(enrichedImage.getThumbnailURL(), is("https://example.org"));
        assertThat(enrichedImage.getPageId(), is("123"));
    }

    @Test
    public void toTaxonImageNonEOL() {
        TaxonImage image = new TaxonImage();

        Taxon taxon = new TaxonImpl("Donald duckus", "ZZZ:123");
        taxon.setCommonNames("bla @en | boo @de");
        taxon.setPath("one | two | three");
        Map<String, String> taxonMap = new TreeMap<String, String>(TaxonUtil.taxonToMap(taxon));
        taxonMap.put(PropertyAndValueDictionary.THUMBNAIL_URL, "http://foo/bar/thumb");
        taxonMap.put(PropertyAndValueDictionary.EXTERNAL_URL, "http://foo/bar");
        TaxonImage enrichedImage = TaxonUtil.enrichTaxonImageWithTaxon(taxonMap, image);

        assertThat(enrichedImage.getCommonName(), is("bla"));
        assertThat(enrichedImage.getTaxonPath(), is("one | two | three"));
        assertThat(enrichedImage.getInfoURL(), is("http://foo/bar"));
        assertThat(enrichedImage.getThumbnailURL(), is("http://foo/bar/thumb"));
        assertThat(enrichedImage.getPageId(), is(nullValue()));
        assertThat(enrichedImage.getImageURL(), is(nullValue()));
    }

    @Test
    public void toPathName() {
        final TaxonImpl taxonA = new TaxonImpl("a", "b");
        taxonA.setPathIds("1 | 2 | 3");
        taxonA.setPathNames("kingdom | family | genus");
        taxonA.setPath("Animalia | Hominidae | Homo");
        final Map<String, String> nameMap = TaxonUtil.toPathNameMap(taxonA, ((Taxon) taxonA).getPath());
        assertThat(nameMap, hasEntry("genus", "Homo"));
        assertThat(nameMap, hasEntry("family", "Hominidae"));
        assertThat(nameMap, hasEntry("kingdom", "Animalia"));

        final String path = TaxonUtil.generateTaxonPath(nameMap);
        assertThat(path, is("Animalia | Hominidae | Homo"));

        final String pathNames = TaxonUtil.generateTaxonPathNames(nameMap);
        assertThat(pathNames, is("kingdom | family | genus"));
    }

    @Test
    public void toPathNameSubSpecific() {
        final TaxonImpl taxonA = new TaxonImpl("a", "b");
        taxonA.setPathIds("1 | 2 | 3 | 4 | 5");
        taxonA.setPathNames("kingdom | family | genus | specificEpithet | subspecificEpithet");
        taxonA.setPath("Animalia | Hominidae | Homo | sapiens | ferus");
        final Map<String, String> nameMap = TaxonUtil.toPathNameMap(taxonA, ((Taxon) taxonA).getPath());
        assertThat(nameMap, hasEntry("genus", "Homo"));
        assertThat(nameMap, hasEntry("family", "Hominidae"));
        assertThat(nameMap, hasEntry("kingdom", "Animalia"));

        final String path = TaxonUtil.generateTaxonPath(nameMap);
        assertThat(path, is("Animalia | Hominidae | Homo"));

        final String pathNames = TaxonUtil.generateTaxonPathNames(nameMap);
        assertThat(pathNames, is("kingdom | family | genus"));
    }

    @Test
    public void nonOverlapping2() {
        final TaxonImpl taxonA = new TaxonImpl("name", "id");
        taxonA.setPath("");
        final TaxonImpl taxonB = new TaxonImpl("otherName", "id");
        taxonB.setPath("three | four");
        final List<Taxon> taxons = TaxonUtil.determineNonOverlappingTaxa(Arrays.asList(taxonA, taxonB));

        assertThat(taxons.size(), is(2));
    }

    @Test
    public void nonOverlapping3() {
        final TaxonImpl taxonA = new TaxonImpl("name", "id");
        taxonA.setPath("");
        final TaxonImpl taxonB = new TaxonImpl("otherName", "id");
        taxonB.setPath("");
        final List<Taxon> taxons = TaxonUtil.determineNonOverlappingTaxa(Arrays.asList(taxonA, taxonB));

        assertThat(taxons.size(), is(2));
    }

    @Test
    public void nonOverlapping() {
        final TaxonImpl taxonA = new TaxonImpl("name", "id");
        taxonA.setPath("one | two");
        final TaxonImpl taxonB = new TaxonImpl("otherName", "id");
        taxonB.setPath("three | four");
        final List<Taxon> taxons = TaxonUtil.determineNonOverlappingTaxa(Arrays.asList(taxonA, taxonB));

        assertThat(taxons.size(), is(2));
    }

    @Test
    public void overlapping() {
        final TaxonImpl taxonA = new TaxonImpl("name", "id");
        taxonA.setPath("one | two | three");
        final TaxonImpl taxonB = new TaxonImpl("otherName", "id");
        taxonB.setPath("one | two");

        final List<Taxon> taxons = TaxonUtil.determineNonOverlappingTaxa(
                Arrays.asList(taxonA, taxonB)
        );

        assertThat(taxons.size(), is(1));
        assertThat(taxons.get(0).getPath(), is("one | two"));
    }

    @Test
    public void overlapping2() {
        final TaxonImpl taxonA = new TaxonImpl("name", "id");
        taxonA.setPath("one | two | three");
        final TaxonImpl taxonB = new TaxonImpl("otherName", "id");
        taxonB.setPath("one | two");
        final TaxonImpl taxonC = new TaxonImpl("otherName", "ids");
        taxonC.setPath("three | four");
        final List<Taxon> taxons = TaxonUtil.determineNonOverlappingTaxa(
                Arrays.asList(taxonA, taxonB, taxonC));

        assertThat(taxons.size(), is(2));
        assertThat(taxons.get(0).getPath(), is("one | two"));
    }


    @Test
    public void toTaxonNameMap() {
        Taxon donald = new TaxonImpl("Donald duckus");
        donald.setPath("Chordata | Mammalia | Artiodactyla | Bovidae | Bovinae | Bos | Bos taurus");
        donald.setPathNames("phylum | class | order | family | subfamily | genus | species");
        Map<String, String> taxonMap = TaxonUtil.toPathNameMap(donald, donald.getPath());

        assertThat(taxonMap.get("phylum"), is("Chordata"));
        assertThat(taxonMap.get("class"), is("Mammalia"));
        assertThat(taxonMap.get("order"), is("Artiodactyla"));
        assertThat(taxonMap.get("family"), is("Bovidae"));
        assertThat(taxonMap.get("subfamily"), is("Bovinae"));
        assertThat(taxonMap.get("genus"), is("Bos"));
        assertThat(taxonMap.get("species"), is("Bos taurus"));
        assertThat(taxonMap.size(), is(7));
    }

    @Test
    public void maptoPathNameMapn() {
        Map<String, String> taxonMap = new TreeMap<String, String>() {{
            put("sourceTaxonClassName", "Mammalia");
            put("sourceTaxonFamilyName", "Bovidae");
            put("sourceTaxonGenusName", "Bos");
            put("sourceTaxonOrderName", "Artiodactyla");
            put("sourceTaxonPhylumName", "Chordata");
            put("sourceTaxonSpeciesName", "Bos taurus");
            put("sourceTaxonSpecificEpithetName", null);
            put("sourceTaxonSubfamilyName", "Bovinae");

        }};

        assertThat(TaxonUtil.generateSourceTaxonName(taxonMap), is("Bos taurus"));
        assertThat(TaxonUtil.generateSourceTaxonPath(taxonMap), is("Chordata | Mammalia | Artiodactyla | Bovidae | Bovinae | Bos | Bos taurus"));
        assertThat(TaxonUtil.generateSourceTaxonPathNames(taxonMap), is("phylum | class | order | family | subfamily | genus | species"));
    }

    @Test
    public void generateSourceTaxon() throws IOException {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.SOURCE_TAXON_CLASS, "class");
            }
        };
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("class"));
        properties.put(TaxonUtil.SOURCE_TAXON_ORDER, "order");
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("order"));
        properties.put(TaxonUtil.SOURCE_TAXON_FAMILY, "family");
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("family"));
        properties.put(TaxonUtil.SOURCE_TAXON_GENUS, "genus");
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("genus"));
        properties.put(TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET, "species");
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("genus species"));
        properties.put(TaxonUtil.SOURCE_TAXON_SUBSPECIFIC_EPITHET, "subspecies");
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("genus species subspecies"));
    }

    @Test
    public void generateTargetTaxon() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.TARGET_TAXON_CLASS, "class");
            }
        };
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("class"));
        properties.put(TaxonUtil.TARGET_TAXON_ORDER, "order");
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("order"));
        properties.put(TaxonUtil.TARGET_TAXON_FAMILY, "family");
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("family"));
        properties.put(TaxonUtil.TARGET_TAXON_GENUS, "genus");
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("genus"));
        properties.put(TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET, "species");
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("genus species"));
        properties.put(TaxonUtil.TARGET_TAXON_SUBSPECIFIC_EPITHET, "subspecies");
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("genus species subspecies"));
    }

    @Test
    public void generateTargetTaxonIgnoreEmptyGenus() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.TARGET_TAXON_GENUS, "");
                put(TaxonUtil.TARGET_TAXON_CLASS, "class");
            }
        };
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("class"));
    }

    @Test
    public void generateTargetTaxonPath() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.TARGET_TAXON_GENUS, "genusValue");
                put(TaxonUtil.TARGET_TAXON_CLASS, "classValue");
            }
        };
        assertThat(TaxonUtil.generateTargetTaxonPath(properties), is("classValue | genusValue"));
        assertThat(TaxonUtil.generateTargetTaxonPathNames(properties), is("class | genus"));
    }

    @Test
    public void generateSourceTaxonPath() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.SOURCE_TAXON_GENUS, "aGenus");
                put(TaxonUtil.SOURCE_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateSourceTaxonPath(properties), is("aClass | aGenus"));
        assertThat(TaxonUtil.generateSourceTaxonPathNames(properties), is("class | genus"));
    }

    @Test
    public void generateSourceTaxonPathWithSpecificEpithet() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET, "some epithet");
                put(TaxonUtil.SOURCE_TAXON_GENUS, "aGenus");
                put(TaxonUtil.SOURCE_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateSourceTaxonPath(properties), is("aClass | aGenus | aGenus some epithet"));
        assertThat(TaxonUtil.generateSourceTaxonPathNames(properties), is("class | genus | species"));
    }

    @Test
    public void generateTargetTaxonPathWithSpecificEpithet() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.TARGET_TAXON_SPECIFIC_EPITHET, "some epithet");
                put(TaxonUtil.TARGET_TAXON_SPECIES, "some species");
                put(TaxonUtil.TARGET_TAXON_GENUS, "aGenus");
                put(TaxonUtil.TARGET_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateTargetTaxonPath(properties), is("aClass | aGenus | aGenus some epithet"));
        assertThat(TaxonUtil.generateTargetTaxonPathNames(properties), is("class | genus | species"));
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("aGenus some epithet"));
    }

    @Test
    public void generateSourceTaxonPathWithSpecies() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.SOURCE_TAXON_SPECIES, "some species");
                put(TaxonUtil.SOURCE_TAXON_GENUS, "aGenus");
                put(TaxonUtil.SOURCE_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateSourceTaxonPath(properties), is("aClass | aGenus | some species"));
        assertThat(TaxonUtil.generateSourceTaxonPathNames(properties), is("class | genus | species"));
        assertThat(TaxonUtil.generateSourceTaxonName(properties), is("some species"));
    }

    @Test
    public void generateTargetTaxonPathWithSpecies() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.TARGET_TAXON_SPECIES, "some species");
                put(TaxonUtil.TARGET_TAXON_GENUS, "aGenus");
                put(TaxonUtil.TARGET_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateTargetTaxonPath(properties), is("aClass | aGenus | some species"));
        assertThat(TaxonUtil.generateTargetTaxonPathNames(properties), is("class | genus | species"));
        assertThat(TaxonUtil.generateTargetTaxonName(properties), is("some species"));
    }

    @Test
    public void generateSpeciesName() {
        final HashMap<String, String> properties = new HashMap<String, String>() {
            {
                put(TaxonUtil.SOURCE_TAXON_GENUS, "aGenus");
                put(TaxonUtil.SOURCE_TAXON_CLASS, "aClass");
            }
        };
        assertThat(TaxonUtil.generateSpeciesName(properties,
                TaxonUtil.SOURCE_TAXON_GENUS,
                TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET,
                TaxonUtil.SOURCE_TAXON_SUBSPECIFIC_EPITHET,
                TaxonUtil.SOURCE_TAXON_SPECIES), is(nullValue()));
    }

    @Test
    public void enrichTaxonNames2() {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put("sourceTaxonGenus", "some_genus");
            put("sourceTaxonGenusId", "id:1234");
        }};
        Map<String, String> enriched = TaxonUtil.enrichTaxonNames(properties);

        assertThat(enriched.get("sourceTaxonGenus"), is("some_genus"));
        assertThat(enriched.get("sourceTaxonGenusName"), is("some_genus"));
        assertThat(enriched.get("sourceTaxonName"), is("some_genus"));
        assertThat(enriched.get("sourceTaxonRank"), is("genus"));
        assertThat(enriched.get("sourceTaxonPath"), is("some_genus"));
        assertThat(enriched.get("sourceTaxonPathNames"), is("genus"));
    }

    @Test
    public void enrichTaxonNamesSubGenus() {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put("sourceTaxonSubgenus", "some_subgenus");
            put("sourceTaxonSubgenusId", "id:12345");
        }};
        Map<String, String> enriched = TaxonUtil.enrichTaxonNames(properties);

        assertThat(enriched.get("sourceTaxonSubgenus"), is("some_subgenus"));
        assertThat(enriched.get("sourceTaxonSubgenusName"), is("some_subgenus"));
        assertThat(enriched.get("sourceTaxonName"), is("some_subgenus"));
        assertThat(enriched.get("sourceTaxonRank"), is("subgenus"));
        assertThat(enriched.get("sourceTaxonPath"), is("some_subgenus"));
        assertThat(enriched.get("sourceTaxonPathNames"), is("subgenus"));
    }

    @Test
    public void enrichTaxonNames4() {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put("sourceTaxonClassName", "some_class");
            put("sourceTaxonSubclass", "some_subclass");
        }};
        Map<String, String> enriched = TaxonUtil.enrichTaxonNames(properties);

        assertThat(enriched.get("sourceTaxonName"), is("some_subclass"));
        assertThat(enriched.get("sourceTaxonPath"), is("some_class | some_subclass"));
        assertThat(enriched.get("sourceTaxonPathNames"), is("class | subclass"));
        assertThat(enriched.get("sourceTaxonRank"), is("subclass"));
    }

    @Test
    public void enrichTaxonNames5() {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put("sourceTaxonSpeciesName", "Homo sapiens");
            put("sourceTaxonSubclass", "some_subclass");
        }};
        Map<String, String> enriched = TaxonUtil.enrichTaxonNames(properties);

        assertThat(enriched.get("sourceTaxonName"), is("Homo sapiens"));
        assertThat(enriched.get("sourceTaxonPath"), is("some_subclass | Homo sapiens"));
        assertThat(enriched.get("sourceTaxonPathNames"), is("subclass | species"));
        assertThat(enriched.get("sourceTaxonRank"), is("species"));
    }

    @Test
    public void enrichTaxonNamesCommonName() {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put("sourceTaxonCommonName", "some_common_name");
        }};
        Map<String, String> enriched = TaxonUtil.enrichTaxonNames(properties);

        assertThat(enriched.get("sourceTaxonCommonName"), is("some_common_name"));
        assertThat(enriched.get("sourceTaxonName"), is("some_common_name"));
        assertThat(enriched.get("sourceTaxonPath"), is("some_common_name"));
    }

    @Test
    public void notEnrichTaxonNamesCommonNameIfOtherNamesArePresent() {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put("sourceTaxonCommonName", "some_common_name");
            put("sourceTaxonSpecies", "some_species_name");
        }};
        Map<String, String> enriched = TaxonUtil.enrichTaxonNames(properties);

        assertThat(enriched.get("sourceTaxonCommonName"), is("some_common_name"));
        assertThat(enriched.get("sourceTaxonName"), is("some_species_name"));
        assertThat(enriched.get("sourceTaxonPath"), is("some_species_name"));
    }

    @Test
    public void enrichTaxonNamesCommonNameShort() {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put("sourceCommonName", "some_common_name");
        }};
        Map<String, String> enriched = TaxonUtil.enrichTaxonNames(properties);

        assertThat(enriched.get("sourceTaxonCommonName"), is("some_common_name"));
        assertThat(enriched.get("sourceCommonName"), is("some_common_name"));
        assertThat(enriched.get("sourceTaxonName"), is("some_common_name"));
        assertThat(enriched.get("sourceTaxonPath"), is("some_common_name"));
    }

    @Test
    public void enrichTaxonNames3() {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put("sourceGenus", "some_genus");
        }};
        Map<String, String> enriched = TaxonUtil.enrichTaxonNames(properties);

        assertThat(enriched.get("sourceTaxonGenusName"), is("some_genus"));
        assertThat(enriched.get("sourceTaxonName"), is("some_genus"));
        assertThat(enriched.get("sourceTaxonPath"), is("some_genus"));
        assertThat(enriched.get("sourceTaxonPathNames"), is("genus"));
    }


    @Test
    public void enrichTaxonNames() {
        HashMap<String, String> properties = new HashMap<String, String>() {{
            put("targetFamilyName", "some_family");
            put("targetOrderName", "some_order");
            put("targetGenusName", null);
            put("targetTaxonName", null);
        }};
        Map<String, String> enriched = TaxonUtil.enrichTaxonNames(properties);

        assertThat(enriched.get("targetOrderName"), is("some_order"));
        assertThat(enriched.get("targetFamilyName"), is("some_family"));

        assertThat(enriched.get("targetTaxonOrderName"), is("some_order"));
        assertThat(enriched.get("targetTaxonFamilyName"), is("some_family"));

        assertThat(enriched.get("targetTaxonPath"), is("some_order | some_family"));
        assertThat(enriched.get("targetTaxonPathNames"), is("order | family"));

        assertThat(enriched.get("targetTaxonName"), is("some_family"));
    }


    @Test
    public void labelSynonymOf1() {
        assertThat(TaxonUtil.expandTaxonColumnNameIfNeeded("sourceGenusId"), is("sourceTaxonGenusId"));
    }

    @Test
    public void labelSynonymOfKingdomId() {
        assertThat(TaxonUtil.expandTaxonColumnNameIfNeeded("sourceKingdomId"), is("sourceTaxonKingdomId"));
    }

    @Test
    public void labelSynonymOfKingdomName() {
        assertThat(TaxonUtil.expandTaxonColumnNameIfNeeded("sourceKingdomName"), is("sourceTaxonKingdomName"));
    }

    @Test
    public void labelSynonymOf2() {
        assertThat(TaxonUtil.expandTaxonColumnNameIfNeeded("sourceTaxonGenusId"), is("sourceTaxonGenusId"));
    }

    @Test
    public void labelSynonymOf3() {
        assertThat(TaxonUtil.expandTaxonColumnNameIfNeeded("sourceGenus"), is("sourceTaxonGenusName"));
    }

    @Test
    public void labelSynonymOf4() {
        assertThat(TaxonUtil.expandTaxonColumnNameIfNeeded("sourceGenusName"), is("sourceTaxonGenusName"));
    }

    @Test
    public void labelSynonymOf5() {
        assertThat(TaxonUtil.expandTaxonColumnNameIfNeeded("sourceTaxonGenus"), is("sourceTaxonGenusName"));
    }

    @Test
    public void labelSynonymOfSpecies() {
        assertThat(TaxonUtil.expandTaxonColumnNameIfNeeded("sourceTaxonSpecies"), is("sourceTaxonSpeciesName"));
    }

    @Test
    public void hasLiteratureReference() {
        TaxonImpl taxon = new TaxonImpl(
                "http://treatment.plazi.org/id/C04787D4FFE1FFCBFF07FAF96581CAEB",
                "http://treatment.plazi.org/id/C04787D4FFE1FFCBFF07FAF96581CAEB"
        );

        assertTrue(TaxonUtil.hasLiteratureReference(taxon));
    }

}