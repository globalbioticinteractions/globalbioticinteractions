package org.eol.globi.service;

import org.eol.globi.domain.TaxonomyProvider;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class WikidataUtilTest {

    @Test
    public void lookupProviders() throws IOException, URISyntaxException {
        Collection<String> providers = WikidataUtil.findTaxonIdProviders();
        final Collection<String> properties = WikidataUtil.PROVIDER_TO_WIKIDATA.values();
        assertThat(properties, everyItem(isIn(providers)));
    }

    @Test
    public void lookupWikiDataToProviders() throws IOException, URISyntaxException {
        Collection<String> providers = WikidataUtil.findTaxonIdProviders();
        final Collection<String> properties = WikidataUtil.WIKIDATA_TO_PROVIDER.keySet();
        assertThat(properties, everyItem(isIn(providers)));
    }

    @Test
    public void lookupTaxonLinks() throws IOException, URISyntaxException {
        Map<TaxonomyProvider, String> relatedTaxonIds =
                WikidataUtil.findRelatedTaxonIds("NCBI:9606");

        assertThat(relatedTaxonIds, is(new TreeMap<TaxonomyProvider, String>() {{
            put(TaxonomyProvider.ITIS,"180092");
            put(TaxonomyProvider.NBN, "NHMSYS0000376773");
            put(TaxonomyProvider.NCBI, "9606");
            put(TaxonomyProvider.EOL, "327955");
            put(TaxonomyProvider.GBIF, "2436436");
            put(TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA, "10857762");
            put(TaxonomyProvider.INATURALIST_TAXON, "43584");
            put(TaxonomyProvider.WIKIDATA, "Q15978631");
            put(TaxonomyProvider.MSW, "12100795");
        }}));
    }

    @Test
    public void lookupTaxonLinksByWDEntry() throws IOException, URISyntaxException {
        Map<TaxonomyProvider, String> relatedTaxonIds =
                WikidataUtil.findRelatedTaxonIds("WD:Q15978631");

        assertThat(relatedTaxonIds, is(new TreeMap<TaxonomyProvider, String>() {{
            put(TaxonomyProvider.ITIS,"180092");
            put(TaxonomyProvider.NBN, "NHMSYS0000376773");
            put(TaxonomyProvider.NCBI, "9606");
            put(TaxonomyProvider.EOL, "327955");
            put(TaxonomyProvider.GBIF, "2436436");
            put(TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA, "10857762");
            put(TaxonomyProvider.INATURALIST_TAXON, "43584");
            put(TaxonomyProvider.WIKIDATA, "Q15978631");
            put(TaxonomyProvider.MSW, "12100795");
        }}));
    }


}