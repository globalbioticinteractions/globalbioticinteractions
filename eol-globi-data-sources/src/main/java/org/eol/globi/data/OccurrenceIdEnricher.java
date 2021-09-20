package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionProcessorAbstract;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class OccurrenceIdEnricher extends InteractionProcessorAbstract {


    public OccurrenceIdEnricher(InteractionListener listener, ImportLogger logger) {
        super(listener, logger);
    }

    static Map<String, String> enrichOccurrenceIdIfPossible(Map<String, String> interactions) {

        Optional<Map<String, String>> enriched = enrichWithReferenceUrl(
                interactions,
                DatasetImporterForTSV.SOURCE_OCCURRENCE_ID,
                DatasetImporterForTSV.SOURCE_INSTITUTION_CODE
        );

        if (!enriched.isPresent()) {
            enriched = enrichWithReferenceUrl(
                    interactions,
                    DatasetImporterForTSV.TARGET_OCCURRENCE_ID,
                    DatasetImporterForTSV.TARGET_INSTITUTION_CODE
            );
        }

        return enriched.orElse(interactions);
    }

    private static Optional<Map<String, String>> enrichWithReferenceUrl(Map<String, String> interactions,
                                                                        String occurrenceIdKey,
                                                                        String institutionCodeKey) {
        Optional<Map<String, String>> enriched = Optional.empty();
        String occId
                = interactions.getOrDefault(occurrenceIdKey, "");
        String institutionCode
                = interactions.getOrDefault(institutionCodeKey, "");
        String referenceUrl
                = interactions.getOrDefault(DatasetImporterForTSV.REFERENCE_URL, "");

        if (StringUtils.equals("F", institutionCode)
                && StringUtils.isNoneBlank(occId)
                && StringUtils.isBlank(referenceUrl)) {
            try {
                URI uri = URI.create("https://db.fieldmuseum.org/" + StringUtils.trim(occId));
                enriched = Optional.of(new TreeMap<String, String>(interactions) {{
                    put(DatasetImporterForTSV.REFERENCE_URL, uri.toString());
                }});
            } catch (IllegalArgumentException ex) {
                // ignore opportunistic url insertion
            }
        }

        return enriched;
    }


    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        emit(enrichOccurrenceIdIfPossible(interaction));
    }
}
