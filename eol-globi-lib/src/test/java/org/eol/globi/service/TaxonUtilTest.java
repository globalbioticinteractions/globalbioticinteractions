package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
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
        src.setStatus(new TermImpl("statusId", "statusLabel"));
        Taxon target = new TaxonImpl();
        TaxonUtil.copy(src, target);
        assertThat(target.getStatus().getId(), is("statusId"));
        assertThat(target.getStatus().getName(), is("statusLabel"));
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
        final Map<String, String> nameMap = TaxonUtil.toPathNameMap(taxonA);
        assertThat(nameMap, hasEntry("genus", "Homo"));
        assertThat(nameMap, hasEntry("family", "Hominidae"));
        assertThat(nameMap, hasEntry("kingdom", "Animalia"));

        final String path = TaxonUtil.generateTaxonPath(nameMap);
        assertThat(path, is("Animalia | Hominidae | Homo"));

        final String pathNames = TaxonUtil.generateTaxonPathNames(nameMap);
        assertThat(pathNames, is("kingdom | family | genus"));
    }

}