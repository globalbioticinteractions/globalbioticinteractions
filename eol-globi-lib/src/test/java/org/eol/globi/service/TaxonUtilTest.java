package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonImpl;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class TaxonUtilTest {

    @Test
    public void homonym() {
        TaxonImpl taxon = new TaxonImpl();
        taxon.setName("Lestes");
        taxon.setPath("Insecta|Lestidae|Lestes");
        taxon.setPathNames("class|family|genus");

        TaxonImpl otherTaxon = new TaxonImpl();
        otherTaxon.setName("Lestes");
        otherTaxon.setPath("Mammalia|Mesonychidae|Lestes");
        otherTaxon.setPathNames("class|family|genus");

        assertThat(TaxonUtil.likelyHomonym(taxon, otherTaxon), is(true));
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
    public void toTaxonImage() {
        TaxonImage image = new TaxonImage();

        Taxon taxon = new TaxonImpl("Donald duckus", "EOL:123");
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
        assertThat(enrichedImage.getPageId(), is("123"));
        assertThat(enrichedImage.getImageURL(), is(nullValue()));
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

}