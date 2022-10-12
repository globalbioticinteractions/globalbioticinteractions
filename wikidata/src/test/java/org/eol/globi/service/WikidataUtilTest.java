package org.eol.globi.service;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.eol.globi.domain.TaxonomyProvider.ATLAS_OF_LIVING_AUSTRALIA;
import static org.eol.globi.domain.TaxonomyProvider.AUSTRALIAN_FAUNAL_DIRECTORY;
import static org.eol.globi.domain.TaxonomyProvider.BATBASE;
import static org.eol.globi.domain.TaxonomyProvider.BATBASE_INTERACTION;
import static org.eol.globi.domain.TaxonomyProvider.BATPLANT;
import static org.eol.globi.domain.TaxonomyProvider.BIODIVERSITY_AUSTRALIA;
import static org.eol.globi.domain.TaxonomyProvider.BOLD_BIN;
import static org.eol.globi.domain.TaxonomyProvider.CATALOGUE_OF_LIFE;
import static org.eol.globi.domain.TaxonomyProvider.ENVO;
import static org.eol.globi.domain.TaxonomyProvider.FISHBASE_CACHE;
import static org.eol.globi.domain.TaxonomyProvider.GEONAMES;
import static org.eol.globi.domain.TaxonomyProvider.GULFBASE;
import static org.eol.globi.domain.TaxonomyProvider.NATIONAL_OCEANOGRAPHIC_DATA_CENTER;
import static org.eol.globi.domain.TaxonomyProvider.OPEN_BIODIV;
import static org.eol.globi.domain.TaxonomyProvider.PLAZI_TAXON_CONCEPT;
import static org.eol.globi.domain.TaxonomyProvider.TERRESTRIAL_PARASITE_TRACKER;
import static org.eol.globi.domain.TaxonomyProvider.WIKIDATA;
import static org.eol.globi.domain.TaxonomyProvider.WIKIPEDIA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.Is.is;

public class WikidataUtilTest {

    @Test
    public void checkSupportForSupportedTaxonProviders() throws IOException, URISyntaxException {
        Collection<TaxonomyProvider> ignored = Arrays.asList(
                CATALOGUE_OF_LIFE,
                GEONAMES,
                WIKIPEDIA,
                ENVO,
                ATLAS_OF_LIVING_AUSTRALIA,
                AUSTRALIAN_FAUNAL_DIRECTORY,
                BIODIVERSITY_AUSTRALIA,
                NATIONAL_OCEANOGRAPHIC_DATA_CENTER,
                WIKIDATA,
                FISHBASE_CACHE,
                BATPLANT,
                BATBASE_INTERACTION,
                BATBASE,
                OPEN_BIODIV,
                GULFBASE,
                BOLD_BIN,
                PLAZI_TAXON_CONCEPT,
                TERRESTRIAL_PARASITE_TRACKER

        );
        List<TaxonomyProvider> unsupported = new ArrayList<>();
        for (TaxonomyProvider provider : TaxonomyProvider.values()) {
            String wdProvider = WikidataUtil.PROVIDER_TO_WIKIDATA.get(provider);
            if (wdProvider == null && !ignored.contains(provider)) {
                unsupported.add(provider);
            }

        }
        assertThat("no mapping for supported taxon providers [" + unsupported.stream().map(TaxonomyProvider::getIdPrefix).collect(Collectors.joining(" ")) + "]"
                , unsupported, is(empty()));
    }


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

        assertThat(ids, is("BOLDTaxon:12439|EOL:327955|GBIF:2436436|INAT_TAXON:43584|IRMNG:10857762|ITIS:180092|MSW:12100795|NBN:NHMSYS0000376773|NCBI:9606|OTT:770315|WD:Q15978631|WORMS:1455977"));

        final String names = relatedTaxonIds
                .stream()
                .map(Taxon::getName)
                .distinct()
                .collect(Collectors.joining("|"));

        assertThat(names, is("Homo sapiens"));
    }

    @Test
    public void lookupTaxonLinksIndexFungorum() throws IOException, URISyntaxException {
        List<Taxon> relatedTaxonIds =
                WikidataUtil.findRelatedTaxonIds("IF:7106");

        final String ids = relatedTaxonIds
                .stream()
                .map(Taxon::getExternalId)
                .sorted()
                .collect(Collectors.joining("|"));

        assertThat(ids, is("EOL:16498|GBIF:2616104|IF:7106|INAT_TAXON:327996|IRMNG:1312559|ITIS:14134|NBN:NHMSYS0001474393|NCBI:5598|OTT:464790|WD:Q133266|WORMS:100208"));

        final String names = relatedTaxonIds
                .stream()
                .map(Taxon::getName)
                .distinct()
                .collect(Collectors.joining("|"));

        assertThat(names, is("Alternaria"));
    }

    @Test
    public void lookupTaxonLinksByWDEntry() throws IOException, URISyntaxException {
        List<Taxon> relatedTaxonIds =
                WikidataUtil.findRelatedTaxonIds("WD:Q15978631");

        final String ids = relatedTaxonIds
                .stream()
                .map(Taxon::getExternalId)
                .sorted()
                .collect(Collectors.joining("|"));

        assertThat(ids, is("BOLDTaxon:12439|EOL:327955|GBIF:2436436|INAT_TAXON:43584|IRMNG:10857762|ITIS:180092|MSW:12100795|NBN:NHMSYS0000376773|NCBI:9606|OTT:770315|WD:Q15978631|WORMS:1455977"));

        final String names = relatedTaxonIds
                .stream()
                .map(Taxon::getName)
                .distinct()
                .collect(Collectors.joining("|"));

        assertThat(names, is("Homo sapiens"));
    }


}