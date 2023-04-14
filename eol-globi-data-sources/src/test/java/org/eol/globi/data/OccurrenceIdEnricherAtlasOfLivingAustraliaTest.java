package org.eol.globi.data;

import org.eol.globi.service.TaxonUtil;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class OccurrenceIdEnricherAtlasOfLivingAustraliaTest {


    @Test
    public void inferALAReferenceURL() {
        // see https://github.com/globalbioticinteractions/padil-bee/issues/1
        TreeMap<String, String> interactions = new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "2c4b4cfb-8ed3-40a9-96b2-30a4df4cdfdc");
            put(DatasetImporterForTSV.SOURCE_RECORD_NUMBER, "HYM 20785");
            put(TaxonUtil.SOURCE_TAXON_ID, "https://biodiversity.org.au/afd/taxa/babd29c2-5e10-447c-8cc0-e3a66e741f5e");
        }};

        Map<String, String> enriched = OccurrenceIdEnricherAtlasOfLivingAustralia.enrichOccurrenceIdIfPossible(interactions);

        assertThat(enriched.get(DatasetImporterForTSV.REFERENCE_URL), is("https://biocache.ala.org.au/occurrences/2c4b4cfb-8ed3-40a9-96b2-30a4df4cdfdc"));
    }


    @Test
    public void inferALAReferenceURLNonEmpty() {
        // see https://github.com/globalbioticinteractions/padil-bee/issues/1
        TreeMap<String, String> interactions = new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "2c4b4cfb-8ed3-40a9-96b2-30a4df4cdfdc");
            put(DatasetImporterForTSV.SOURCE_RECORD_NUMBER, "HYM 20785");
            put(TaxonUtil.SOURCE_TAXON_ID, "https://biodiversity.org.au/afd/taxa/babd29c2-5e10-447c-8cc0-e3a66e741f5e");
            put(DatasetImporterForTSV.REFERENCE_URL, "https://example.org");
        }};

        Map<String, String> enriched = OccurrenceIdEnricherAtlasOfLivingAustralia.enrichOccurrenceIdIfPossible(interactions);

        assertThat(enriched.get(DatasetImporterForTSV.REFERENCE_URL), is("https://example.org"));
    }

}