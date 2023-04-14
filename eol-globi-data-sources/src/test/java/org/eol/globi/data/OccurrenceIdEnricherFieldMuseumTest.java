package org.eol.globi.data;

import org.eol.globi.service.TaxonUtil;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class OccurrenceIdEnricherFieldMuseumTest {

    @Test
    public void addFieldMuseumOccurrenceIdURL() {
        Map<String, String> interactions = new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "123");
            put(DatasetImporterForTSV.SOURCE_INSTITUTION_CODE, "F");
        }};

        Map<String, String> enriched = OccurrenceIdEnricherFieldMuseum.enrichOccurrenceIdIfPossible(interactions);

        assertThat(enriched.get(DatasetImporterForTSV.REFERENCE_URL), is("https://db.fieldmuseum.org/123"));
    }

    @Test
    public void addFieldMuseumTargetOccurrenceIdURL() {
        Map<String, String> interactions = new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "123");
            put(DatasetImporterForTSV.TARGET_INSTITUTION_CODE, "F");
        }};

        Map<String, String> enriched = OccurrenceIdEnricherFieldMuseum.enrichOccurrenceIdIfPossible(interactions);

        assertThat(enriched.get(DatasetImporterForTSV.REFERENCE_URL), is("https://db.fieldmuseum.org/123"));
    }

    @Test
    public void addFieldMuseumTargetOccurrenceIdURLExistingReferenceUrl() {
        TreeMap<String, String> interactions = new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "123");
            put(DatasetImporterForTSV.TARGET_INSTITUTION_CODE, "F");
            put(DatasetImporterForTSV.REFERENCE_URL, "https://example.org");
        }};

        Map<String, String> enriched = OccurrenceIdEnricherFieldMuseum.enrichOccurrenceIdIfPossible(interactions);

        assertThat(enriched.get(DatasetImporterForTSV.REFERENCE_URL), is("https://example.org"));
    }

}