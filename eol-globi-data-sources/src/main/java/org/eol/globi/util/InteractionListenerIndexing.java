package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

public class InteractionListenerIndexing implements InteractionListener {
    private final Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds;

    public InteractionListenerIndexing(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds) {
        this.interactionsWithUnresolvedOccurrenceIds = interactionsWithUnresolvedOccurrenceIds;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        String sourceOccurrenceId = getOccurrenceId(interaction, DatasetImporterForTSV.SOURCE_OCCURRENCE_ID);
        if (interactionsWithUnresolvedOccurrenceIds.containsKey(
                Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, sourceOccurrenceId))) {
            interactionsWithUnresolvedOccurrenceIds.put(
                    Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, sourceOccurrenceId),
                    InteractionListenerResolving.mapSourceToSource(interaction));
        } else if (interactionsWithUnresolvedOccurrenceIds.containsKey(
                Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, sourceOccurrenceId))) {
            interactionsWithUnresolvedOccurrenceIds.put(
                    Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, sourceOccurrenceId),
                    InteractionListenerResolving.mapSourceToTarget(interaction));
        }

        String targetOccurrenceId = getOccurrenceId(interaction, DatasetImporterForTSV.TARGET_OCCURRENCE_ID);
        if (interactionsWithUnresolvedOccurrenceIds.containsKey(
                Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, targetOccurrenceId))) {
            interactionsWithUnresolvedOccurrenceIds.put(
                    Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, targetOccurrenceId),
                    InteractionListenerResolving.mapTargetToTarget(interaction));
        } else if (interactionsWithUnresolvedOccurrenceIds.containsKey(
                Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, targetOccurrenceId))) {
            interactionsWithUnresolvedOccurrenceIds.put(
                    Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, targetOccurrenceId),
                    InteractionListenerResolving.mapTargetToSource(interaction));
        }

    }

    public static String getOccurrenceId(Map<String, String> interaction, String sourceOccurrenceId) {
        String value = interaction.get(sourceOccurrenceId);

        if (StringUtils.startsWith(value, "http://arctos.database.museum/guid/")) {
            String[] splitValue = StringUtils.split(value, "?");
            value = splitValue.length == 1 ? value : splitValue[0];
        }
        return value;
    }
}
