package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TaxonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.util.InteractionListenerIndexing.getOccurrenceId;

public class InteractionListenerResolving implements InteractionListener {
    private final Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceOrTaxonIds;
    private final InteractionListener interactionListener;

    public InteractionListenerResolving(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceOrTaxonIds, InteractionListener interactionListener) {
        this.interactionsWithUnresolvedOccurrenceOrTaxonIds = interactionsWithUnresolvedOccurrenceOrTaxonIds;
        this.interactionListener = interactionListener;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        final List<Map<String, String>> enrichedProperties = resolveOccurrenceIdsIfPossible(interaction);

        if (enrichedProperties == null) {
            interactionListener.on(interaction);
        } else {
            TreeMap<String, String> enriched = new TreeMap<>(interaction);
            enrichedProperties.forEach(enriched::putAll);
            interactionListener.on(enriched);
        }
    }

    public List<Map<String, String>> resolveOccurrenceIdsIfPossible(Map<String, String> interaction) {
        List<Map<String, String>> enrichedProperties = null;

        if (InteractionListenerCollectUnresolvedOccurrenceIds.hasUnresolvedTargetOccurrenceId(interaction)) {
            String targetOccurrenceId = getOccurrenceId(interaction, DatasetImporterForTSV.TARGET_OCCURRENCE_ID);
            Map<String, String> resolved = interactionsWithUnresolvedOccurrenceOrTaxonIds.get(Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, targetOccurrenceId));
            if (resolved != null && !resolved.isEmpty()) {
                enrichedProperties = new ArrayList<>();
                enrichedProperties.add(resolved);
            }
        }

        if (InteractionListenerCollectUnresolvedOccurrenceIds.hasUnresolvedTargetTaxonId(interaction)) {
            String targetTaxonId = getOccurrenceId(interaction, TaxonUtil.TARGET_TAXON_ID);
            Map<String, String> resolved = interactionsWithUnresolvedOccurrenceOrTaxonIds.get(Pair.of(TaxonUtil.TARGET_TAXON_ID, targetTaxonId));
            if (resolved != null && !resolved.isEmpty()) {
                enrichedProperties = new ArrayList<>();
                enrichedProperties.add(resolved);
            }
        }

        if (InteractionListenerCollectUnresolvedOccurrenceIds.hasUnresolvedSourceOccurrenceId(interaction)) {
            String sourceOccurrenceId = getOccurrenceId(interaction, DatasetImporterForTSV.SOURCE_OCCURRENCE_ID);
            Map<String, String> resolved = interactionsWithUnresolvedOccurrenceOrTaxonIds.get(Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, sourceOccurrenceId));
            if (resolved != null && !resolved.isEmpty()) {
                if (enrichedProperties == null) {
                    enrichedProperties = new ArrayList<>();
                }
                enrichedProperties.add(resolved);
            }
        }

        if (InteractionListenerCollectUnresolvedOccurrenceIds.hasUnresolvedSourceTaxonId(interaction)) {
            String sourceTaxonId = getOccurrenceId(interaction, TaxonUtil.SOURCE_TAXON_ID);
            Map<String, String> resolved = interactionsWithUnresolvedOccurrenceOrTaxonIds.get(Pair.of(TaxonUtil.SOURCE_TAXON_ID, sourceTaxonId));
            if (resolved != null && !resolved.isEmpty()) {
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
