package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionProcessorAbstract;
import org.eol.globi.service.TaxonUtil;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

public class OccurrenceIdEnricherAtlasOfLivingAustralia extends InteractionProcessorAbstract {

    public OccurrenceIdEnricherAtlasOfLivingAustralia(InteractionListener listener, ImportLogger logger) {
        super(listener, logger);
    }

    static Map<String, String> enrichOccurrenceIdIfPossible(Map<String, String> interactions) {

        Optional<Map<String, String>> enriched = Optional.empty();
        if (!interactions.containsKey(DatasetImporterForTSV.REFERENCE_URL)) {
            String occId
                    = interactions.getOrDefault(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "");
            String recordNumber
                    = interactions.getOrDefault(DatasetImporterForTSV.SOURCE_RECORD_NUMBER, "");
            String taxonId
                    = interactions.getOrDefault(TaxonUtil.SOURCE_TAXON_ID, "");

            if (StringUtils.isNotBlank(recordNumber)
                    && StringUtils.isNoneBlank(occId)
                    && StringUtils.contains(taxonId, "https://biodiversity.org.au/afd/taxa/")) try {
                if (isUUID(occId)) {
                    URI uri = URI.create("https://biocache.ala.org.au/occurrences/" + StringUtils.trim(occId));
                    enriched = Optional.of(new TreeMap<String, String>(interactions) {{
                        put(DatasetImporterForTSV.REFERENCE_URL, uri.toString());
                    }});
                }
            } catch (IllegalArgumentException ex) {
                // thrown when
            }

        }

        return enriched.orElse(interactions);
    }

    private static boolean isUUID(String occId) {
        try {
            UUID.fromString(occId);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        emit(enrichOccurrenceIdIfPossible(interaction));
    }
}
