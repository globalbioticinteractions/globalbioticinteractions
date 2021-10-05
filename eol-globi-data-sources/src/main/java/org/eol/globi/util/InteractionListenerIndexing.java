package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InteractionListenerIndexing implements InteractionListener {
    public static final Pattern US_NATIONAL_PARASITE_COLLECTION_RECORD_NUMBER = Pattern.compile("USNPC # [0]*([0-9]+)");
    private final Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds;

    public InteractionListenerIndexing(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds) {
        this.interactionsWithUnresolvedOccurrenceIds = interactionsWithUnresolvedOccurrenceIds;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {

        attemptToResolveSourceOccurrenceId(interaction, getOccurrenceId(interaction, DatasetImporterForTSV.SOURCE_OCCURRENCE_ID));
        inferOccurrenceId(interaction.get(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER))
                .forEach(occurrenceId -> attemptToResolveSourceOccurrenceId(interaction, occurrenceId));

        attemptToResolveTargetOccurrenceId(interaction, getOccurrenceId(interaction, DatasetImporterForTSV.TARGET_OCCURRENCE_ID));
        inferOccurrenceId(interaction.get(DatasetImporterForTSV.TARGET_CATALOG_NUMBER))
                .forEach(occurrenceId -> attemptToResolveTargetOccurrenceId(interaction, occurrenceId));

    }

    public void attemptToResolveTargetOccurrenceId(Map<String, String> interaction, String targetOccurrenceId) {
        if (interactionsWithUnresolvedOccurrenceIds.containsKey(
                Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, targetOccurrenceId))) {
            Map<String, String> enriched = InteractionListenerResolving.mapTargetToTarget(interaction);
            if (enriched.size() > 1) {
                interactionsWithUnresolvedOccurrenceIds.put(
                        Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, targetOccurrenceId),
                        enriched);
            }
        } else if (interactionsWithUnresolvedOccurrenceIds.containsKey(
                Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, targetOccurrenceId))) {
            Map<String, String> enriched = InteractionListenerResolving.mapTargetToSource(interaction);
            if (enriched.size() > 1) {
                interactionsWithUnresolvedOccurrenceIds.put(
                        Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, targetOccurrenceId),
                        enriched);
            }
        }
    }

    public void attemptToResolveSourceOccurrenceId(Map<String, String> interaction, String sourceOccurrenceId) {
        if (interactionsWithUnresolvedOccurrenceIds.containsKey(
                Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, sourceOccurrenceId))) {
            Map<String, String> enriched = InteractionListenerResolving.mapSourceToSource(interaction);
            System.out.println("[" + sourceOccurrenceId + "] resolving");
            if (enriched.size() > 1) {
                System.out.println("[" + sourceOccurrenceId + "] resolved");
                interactionsWithUnresolvedOccurrenceIds.put(
                        Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, sourceOccurrenceId),
                        enriched);
            }
            System.out.println("[" + sourceOccurrenceId + "] not resolved");
        } else if (interactionsWithUnresolvedOccurrenceIds.containsKey(
                Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, sourceOccurrenceId))) {
            Map<String, String> enriched = InteractionListenerResolving.mapSourceToTarget(interaction);
            System.out.println("[" + sourceOccurrenceId + "] resolving");
            if (enriched.size() > 1) {
                System.out.println("[" + sourceOccurrenceId + "] resolved");
                interactionsWithUnresolvedOccurrenceIds.put(
                        Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, sourceOccurrenceId),
                        enriched);
            }
            System.out.println("[" + sourceOccurrenceId + "] not resolved");
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

    static List<String> inferOccurrenceId(String catalogNumber) {
        List<String> occurrenceIds = Collections.emptyList();
        if (StringUtils.isNotBlank(catalogNumber)) {
            Matcher matcher = US_NATIONAL_PARASITE_COLLECTION_RECORD_NUMBER.matcher(catalogNumber);
            if (matcher.find()) {
                String group = matcher.group(1);
                occurrenceIds = new ArrayList<String>() {{
                    String paddedGroup = String.format("%06d", Integer.parseInt(group));
                    add("United States National Parasite Collection USNPC " + paddedGroup);
                    add("United States National Parasite Collection USNPC " + group);
                    add("United States National Parasite Collection " + paddedGroup);
                    add("United States National Parasite Collection " + group);
                }};
            }
        }
        return occurrenceIds;
    }
}
