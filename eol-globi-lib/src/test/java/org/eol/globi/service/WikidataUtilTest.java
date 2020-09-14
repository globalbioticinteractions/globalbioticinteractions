package org.eol.globi.service;

import org.eol.globi.domain.Taxon;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

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
        List<Taxon> relatedTaxonIds =
                WikidataUtil.findRelatedTaxonIds("NCBI:9606");

        final String ids = relatedTaxonIds
                .stream()
                .map(Taxon::getExternalId)
                .sorted()
                .collect(Collectors.joining("|"));
        assertThat(ids, is("EOL:327955|GBIF:2436436|INAT_TAXON:43584|IRMNG:10857762|ITIS:180092|MSW:12100795|NBN:NHMSYS0000376773|NCBI:9606|WD:Q15978631"));

        final String names = relatedTaxonIds.stream().map(Taxon::getName).distinct().collect(Collectors.joining("|"));
        assertThat(names, is("Homo sapiens"));
    }

    @Test
    public void lookupTaxonLinksByWDEntry() throws IOException, URISyntaxException {
        List<Taxon> relatedTaxonIds =
                WikidataUtil.findRelatedTaxonIds("WD:Q15978631");

        final String ids = relatedTaxonIds.stream().map(Taxon::getExternalId).sorted().collect(Collectors.joining("|"));
        assertThat(ids, is("EOL:327955|GBIF:2436436|INAT_TAXON:43584|IRMNG:10857762|ITIS:180092|MSW:12100795|NBN:NHMSYS0000376773|NCBI:9606|WD:Q15978631"));

        final String names = relatedTaxonIds.stream().map(Taxon::getName).distinct().collect(Collectors.joining("|"));
        assertThat(names, is("Homo sapiens"));
    }


}