package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;

import java.util.Map;
import java.util.TreeMap;

public class InteractionListenerResolving implements InteractionListener {
    private final Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds;
    private final InteractionListener interactionListener;

    public InteractionListenerResolving(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, InteractionListener interactionListener) {
        this.interactionsWithUnresolvedOccurrenceIds = interactionsWithUnresolvedOccurrenceIds;
        this.interactionListener = interactionListener;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        Map<String, String> enrichedProperties = null;
         if (interaction.containsKey(DatasetImporterForTSV.TARGET_OCCURRENCE_ID)) {
            String targetOccurrenceId = interaction.get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID);
            Map<String, String> targetProperties = interactionsWithUnresolvedOccurrenceIds.get(Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, targetOccurrenceId));
            if (targetProperties != null) {
                TreeMap<String, String> enrichedMap = new TreeMap<>(interaction);
                mapSourceToTarget(targetProperties, enrichedMap);
                enrichedProperties = enrichedMap;
            }
        }
        interactionListener.on(enrichedProperties == null ? interaction : enrichedProperties);
    }

    private void mapSourceToTarget(Map<String, String> interaction, TreeMap<String, String> enrichedMap) {
        DatasetImporterForTSV.SOURCE_TARGET_PROPERTY_NAME_PAIRS.forEach(pair -> {
            enrichProperties(interaction, enrichedMap, pair.getLeft(), pair.getRight());
        });
    }

    private void enrichProperties(Map<String, String> targetProperties, TreeMap<String, String> enrichedMap, String sourceKey, String targetKey) {
        String value = targetProperties.get(sourceKey);
        if (StringUtils.isNotBlank(value)) {
            enrichedMap.put(targetKey, value);
        }
    }
}
