package org.eol.globi.data;

import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SpecimenCitationEnricherTest {

    @Test
    public void addUSNMOccurrenceIdURL() {
        Map<String, String> interactions = new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER, "123");
            put(DatasetImporterForTSV.SOURCE_INSTITUTION_CODE, "USNM");
            put(DatasetImporterForTSV.SOURCE_COLLECTION_CODE, "urn:uuid:18e3cd08-a962-4f0a-b72c-9a0b3600c5ad");
            put(DatasetImporterForTSV.REFERENCE_CITATION, "some citation");
        }};

        Map<String, String> enriched = SpecimenCitationEnricher.enrichCitationIfPossible(interactions);

        assertThat(enriched.get(DatasetImporterForTSV.REFERENCE_CITATION), is("USNMENT123 some citation"));
    }

    @Test
    public void addUSNMOccurrenceIdURL2() {
        Map<String, String> interactions = new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER, "123");
            put(DatasetImporterForTSV.SOURCE_INSTITUTION_CODE, "USNM");
            put(DatasetImporterForTSV.SOURCE_COLLECTION_CODE, "Entomology");
            put(DatasetImporterForTSV.REFERENCE_CITATION, "some citation");
        }};

        Map<String, String> enriched = SpecimenCitationEnricher.enrichCitationIfPossible(interactions);

        assertThat(enriched.get(DatasetImporterForTSV.REFERENCE_CITATION), is("USNMENT123 some citation"));
    }

    @Test
    public void addUCSBIZCIdURL() {
        Map<String, String> interactions = new TreeMap<String, String>() {{
                put(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER, "123");
            put(DatasetImporterForTSV.SOURCE_INSTITUTION_CODE, "UCSB");
            put(DatasetImporterForTSV.SOURCE_COLLECTION_CODE, "IZC");
            put(DatasetImporterForTSV.REFERENCE_CITATION, "some citation");
        }};

        Map<String, String> enriched = SpecimenCitationEnricher.enrichCitationIfPossible(interactions);

        assertThat(enriched.get(DatasetImporterForTSV.REFERENCE_CITATION), is("123 some citation"));
    }


}