package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.util.InteractionListenerIndexing.getOccurrenceId;

public class InteractionListenerResolving implements InteractionListener {
    private final Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds;
    private final InteractionListener interactionListener;

    public InteractionListenerResolving(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds, InteractionListener interactionListener) {
        this.interactionsWithUnresolvedOccurrenceIds = interactionsWithUnresolvedOccurrenceIds;
        this.interactionListener = interactionListener;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        final List<Map<String, String>> enrichedProperties = resolveOccurrenceIdsIfPossible(interaction);

        if (enrichedProperties == null) {
            interactionListener.on(interaction);
        } else {
            final TreeMap<String, String> interaction1 = new TreeMap<>(interaction);
            enrichedProperties.forEach(interaction1::putAll);
            interactionListener.on(interaction1);
        }
    }

    public List<Map<String, String>> resolveOccurrenceIdsIfPossible(Map<String, String> interaction) {
        List<Map<String, String>> enrichedProperties = null;

        if (InteractionListenerCollectUnresolvedOccurrenceIds.hasUnresolvedTargetOccurrenceId(interaction)) {
            String targetOccurrenceId = getOccurrenceId(interaction, DatasetImporterForTSV.TARGET_OCCURRENCE_ID);
            Map<String, String> resolved = interactionsWithUnresolvedOccurrenceIds.get(Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, targetOccurrenceId));
            if (resolved != null) {
                enrichedProperties = new ArrayList<>();
                enrichedProperties.add(resolved);
            }
        }

        if (InteractionListenerCollectUnresolvedOccurrenceIds.hasUnresolvedSourceOccurrenceId(interaction)) {
            String sourceOccurrenceId = getOccurrenceId(interaction, DatasetImporterForTSV.SOURCE_OCCURRENCE_ID);
            Map<String, String> resolved = interactionsWithUnresolvedOccurrenceIds.get(Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, sourceOccurrenceId));
            if (resolved != null) {
                if (enrichedProperties == null) {
                    enrichedProperties = new ArrayList<>();
                }
                enrichedProperties.add(resolved);
            }
        }
        return enrichedProperties;
    }


    public static Map<String, String> mapSourceToTarget(Map<String, String> interaction) {
        final Map<String, String> enrichedMap = new TreeMap<>();
        DatasetImporterForTSV.SOURCE_TARGET_PROPERTY_NAME_PAIRS.forEach(pair -> {
            enrichProperties(interaction, enrichedMap, pair.getLeft(), pair.getRight());
        });
        return enrichedMap;
    }

    public static Map<String, String> mapTargetToSource(Map<String, String> interaction) {
        final Map<String, String> enrichedMap = new TreeMap<>();
        DatasetImporterForTSV.SOURCE_TARGET_PROPERTY_NAME_PAIRS.forEach(pair -> {
            enrichProperties(interaction, enrichedMap, pair.getRight(), pair.getLeft());
        });
        return enrichedMap;
    }

    public static Map<String, String> mapTargetToTarget(Map<String, String> interaction) {
        final Map<String, String> enrichedMap = new TreeMap<>();
        DatasetImporterForTSV.SOURCE_TARGET_PROPERTY_NAME_PAIRS.forEach(pair -> {
            enrichProperties(interaction, enrichedMap, pair.getRight(), pair.getRight());
        });
        return enrichedMap;
    }

    public static Map<String, String> mapSourceToSource(Map<String, String> interaction) {
        final Map<String, String> enrichedMap = new TreeMap<>();
        DatasetImporterForTSV.SOURCE_TARGET_PROPERTY_NAME_PAIRS.forEach(pair -> {
            enrichProperties(interaction, enrichedMap, pair.getLeft(), pair.getLeft());
        });
        return enrichedMap;
    }

    private static void enrichProperties(Map<String, String> targetProperties, Map<String, String> enrichedMap, String sourceKey, String targetKey) {
        String value = targetProperties.get(sourceKey);
        if (StringUtils.isNotBlank(value)) {
            enrichedMap.put(targetKey, value);
        }
    }
}
