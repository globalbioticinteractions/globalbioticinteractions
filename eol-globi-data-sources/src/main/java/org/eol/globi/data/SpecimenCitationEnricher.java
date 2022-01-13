package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionProcessorAbstract;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class SpecimenCitationEnricher extends InteractionProcessorAbstract {


    public SpecimenCitationEnricher(InteractionListener listener, ImportLogger logger) {
        super(listener, logger);
    }

    static Map<String, String> enrichCitationIfPossible(Map<String, String> interactions) {

        Optional<Map<String, String>> enriched = enrichCitation(
                interactions,
                DatasetImporterForTSV.SOURCE_CATALOG_NUMBER,
                DatasetImporterForTSV.SOURCE_COLLECTION_CODE,
                DatasetImporterForTSV.SOURCE_INSTITUTION_CODE
        );

        if (!enriched.isPresent()) {
            enriched = enrichCitation(
                    interactions,
                    DatasetImporterForTSV.TARGET_CATALOG_NUMBER,
                    DatasetImporterForTSV.TARGET_COLLECTION_CODE,
                    DatasetImporterForTSV.TARGET_INSTITUTION_CODE
            );
        }

        return enriched.orElse(interactions);
    }

    private static Optional<Map<String, String>> enrichCitation(Map<String, String> interactions,
                                                                String catalogueNumberKey,
                                                                String collectionCodeKey,
                                                                String institutionCodeKey) {
        Optional<Map<String, String>> enriched = Optional.empty();
        String catalogueNumber
                = interactions.getOrDefault(catalogueNumberKey, "");
        String institutionCode
                = interactions.getOrDefault(institutionCodeKey, "");
        String collectionCode
                = interactions.getOrDefault(collectionCodeKey, "");
        String referenceCitation
                = interactions.getOrDefault(DatasetImporterForTSV.REFERENCE_CITATION, "");

        if (StringUtils.equalsIgnoreCase(collectionCode, "urn:uuid:18e3cd08-a962-4f0a-b72c-9a0b3600c5ad")
                && StringUtils.equalsIgnoreCase(institutionCode, "USNM")
                && StringUtils.isNoneBlank(catalogueNumber)) {
            try {
                enriched = Optional.of(new TreeMap<String, String>(interactions) {{
                    String specimenCitation = "USNMENT" + StringUtils.trim(catalogueNumber);
                    put(DatasetImporterForTSV.REFERENCE_CITATION, StringUtils.prependIfMissing(referenceCitation, specimenCitation + " "));
                }});
            } catch (IllegalArgumentException ex) {
                // ignore opportunistic url insertion
            }
        }

        return enriched;
    }


    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        emit(enrichCitationIfPossible(interaction));
    }
}
