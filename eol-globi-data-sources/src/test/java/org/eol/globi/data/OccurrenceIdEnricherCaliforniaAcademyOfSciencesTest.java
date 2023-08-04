package org.eol.globi.data;

import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class OccurrenceIdEnricherCaliforniaAcademyOfSciencesTest {

    @Test
    public void addCASURL() {
        Map<String, String> interactions = new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER, "123");
            put(DatasetImporterForTSV.SOURCE_INSTITUTION_CODE, "CAS");
        }};

        Map<String, String> enriched = OccurrenceIdEnricherCaliforniaAcademyOfSciences.enrichOccurrenceIdIfPossible(interactions);

        assertThat(enriched.get(DatasetImporterForTSV.REFERENCE_URL), is("https://monarch.calacademy.org/collections/list.php?catnum=123"));
    }

    @Test
    public void addCASTargetOccurrenceIdURL() {
        Map<String, String> interactions = new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.TARGET_CATALOG_NUMBER, "123");
            put(DatasetImporterForTSV.TARGET_INSTITUTION_CODE, "CAS");
        }};

        Map<String, String> enriched = OccurrenceIdEnricherCaliforniaAcademyOfSciences.enrichOccurrenceIdIfPossible(interactions);

        assertThat(enriched.get(DatasetImporterForTSV.REFERENCE_URL), is("https://monarch.calacademy.org/collections/list.php?catnum=123"));
    }

    @Test
    public void addCASTargetCatalogNumberURLExistingReferenceUrl() {
        TreeMap<String, String> interactions = new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.TARGET_CATALOG_NUMBER, "123");
            put(DatasetImporterForTSV.TARGET_INSTITUTION_CODE, "CAS");
            put(DatasetImporterForTSV.REFERENCE_URL, "https://example.org");
        }};

        Map<String, String> enriched = OccurrenceIdEnricherCaliforniaAcademyOfSciences.enrichOccurrenceIdIfPossible(interactions);

        assertThat(enriched.get(DatasetImporterForTSV.REFERENCE_URL), is("https://example.org"));
    }

}