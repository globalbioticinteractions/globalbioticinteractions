package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
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

import static junit.framework.TestCase.assertNotNull;
import static org.eol.globi.domain.TaxonomyProvider.ATLAS_OF_LIVING_AUSTRALIA;
import static org.eol.globi.domain.TaxonomyProvider.AUSTRALIAN_FAUNAL_DIRECTORY;
import static org.eol.globi.domain.TaxonomyProvider.BATBASE;
import static org.eol.globi.domain.TaxonomyProvider.BATBASE_INTERACTION;
import static org.eol.globi.domain.TaxonomyProvider.BATPLANT;
import static org.eol.globi.domain.TaxonomyProvider.BIODIVERSITY_AUSTRALIA;
import static org.eol.globi.domain.TaxonomyProvider.BOLD_BIN;
import static org.eol.globi.domain.TaxonomyProvider.ENVO;
import static org.eol.globi.domain.TaxonomyProvider.FISHBASE_CACHE;
import static org.eol.globi.domain.TaxonomyProvider.GEONAMES;
import static org.eol.globi.domain.TaxonomyProvider.GULFBASE;
import static org.eol.globi.domain.TaxonomyProvider.NATIONAL_OCEANOGRAPHIC_DATA_CENTER;
import static org.eol.globi.domain.TaxonomyProvider.OPEN_BIODIV;
import static org.eol.globi.domain.TaxonomyProvider.PLAZI_TAXON_CONCEPT;
import static org.eol.globi.domain.TaxonomyProvider.WIKIDATA;
import static org.eol.globi.domain.TaxonomyProvider.WIKIPEDIA;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class WikidataUtilTest {

    @Test
    public void verifyAvailableTaxonProviders() throws IOException, URISyntaxException {
        Collection<String> providers = WikidataUtil.findTaxonIdProviders();
        String[] s = StringUtils.split("P627 P687 P838 P842 P960 P961 P962 P1070 P1076 P1348 P1421 P1727 P1743 P1744 P1745 P1746 P1747 P1761 P1772 P1832 P1895 P1939 P1940 P1991 P2024 P2026 P2036 P2040 P2426 P2434 P2455 P2464 P2752 P2794 P2809 P2833 P2946 P3031 P3060 P3064 P3088 P3099 P3100 P3101 P3102 P3105 P3130 P3186 P3288 P3322 P3380 P3398 P3405 P3420 P3444 P3459 P3591 P3594 P3606 P3746 P3795 P4024 P4122 P4125 P4194 P4301 P4311 P4433 P4526 P4567 P4630 P4664 P4715 P4728 P4753 P4758 P4798 P4807 P4855 P4902 P5003 P5036 P5037 P5179 P5216 P5221 P5257 P5263 P5299 P5354 P5397 P5473 P5626 P5683 P5698 P5862 P5864 P5945 P5953 P5984 P6003 P6019 P6021 P6025 P6028 P6030 P6033 P6034 P6035 P6036 P6039 P6040 P6041 P6042 P6043 P6044 P6045 P6046 P6047 P6048 P6049 P6050 P6051 P6052 P6053 P6054 P6055 P6056 P6057 P6061 P6092 P6093 P6094 P6096 P6098 P6101 P6103 P6105 P6114 P6115 P6128 P6137 P6139 P6142 P6159 P6161 P6163 P6176 P6177 P6209 P6227 P6268 P6285 P6289 P6341 P6347 P6349 P6366 P6376 P6408 P6433 P6481 P6485 P6487 P6516 P6704 P6756 P6864 P6904 P6933 P6982 P7066 P7090 P7202 P7224 P7254 P7255 P7472 P7496 P7546 P7552 P7743 P7905 P8061 P8145 P8164 P8468 P8469 P8660 P8707 P8724 P8765 P8792 P8892 P8915 P9076 P9093 P9157 P9243 P9408 P9423 P9460 P9501 P9503 P9576 P9580 P9595 P9596 P9608 P9649 P9654 P9684 P9685 P9690 P9691 P9741 P9799 P9839 P9876 P9889 P9908 P9932 P9967 P10003 P10007 P10064", " ");
        Collection<String> ignored = Arrays.asList(s);
        List<String> unsupported = new ArrayList<>();
        for (String provider : providers) {
            TaxonomyProvider taxonomyProvider = WikidataUtil.WIKIDATA_TO_PROVIDER.get(provider);
            if (taxonomyProvider == null && !ignored.contains(provider)) {
                unsupported.add(provider);
            }

        }
        assertThat("no mapping for wikidata taxon providers [" + unsupported.stream().collect(Collectors.joining(" ")) + "]"
                , unsupported, is(empty()));
    }

    @Test
    public void checkSupportForSupportedTaxonProviders() throws IOException, URISyntaxException {
        Collection<TaxonomyProvider> ignored = Arrays.asList(
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
                PLAZI_TAXON_CONCEPT

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

        final String names = relatedTaxonIds.stream().map(Taxon::getName).distinct().collect(Collectors.joining("|"));
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

        final String names = relatedTaxonIds.stream().map(Taxon::getName).distinct().collect(Collectors.joining("|"));
        assertThat(names, is("Alternaria"));
    }

    @Test
    public void lookupTaxonLinksByWDEntry() throws IOException, URISyntaxException {
        List<Taxon> relatedTaxonIds =
                WikidataUtil.findRelatedTaxonIds("WD:Q15978631");

        final String ids = relatedTaxonIds.stream().map(Taxon::getExternalId).sorted().collect(Collectors.joining("|"));
        assertThat(ids, is("BOLDTaxon:12439|EOL:327955|GBIF:2436436|INAT_TAXON:43584|IRMNG:10857762|ITIS:180092|MSW:12100795|NBN:NHMSYS0000376773|NCBI:9606|OTT:770315|WD:Q15978631|WORMS:1455977"));

        final String names = relatedTaxonIds.stream().map(Taxon::getName).distinct().collect(Collectors.joining("|"));
        assertThat(names, is("Homo sapiens"));
    }


}