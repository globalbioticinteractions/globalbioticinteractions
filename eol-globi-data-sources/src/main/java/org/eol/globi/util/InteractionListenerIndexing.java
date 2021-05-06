package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class InteractionListenerIndexing implements InteractionListener {
    private final Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds;
    private final Predicate<Map<String, String>> shouldIndex;

    public InteractionListenerIndexing(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds) {
        this(interactionsWithUnresolvedOccurrenceIds, new Predicate<Map<String, String>>() {

            @Override
            public boolean test(Map<String, String> interaction) {
                return interaction.containsKey(DatasetImporterForTSV.TARGET_OCCURRENCE_ID)
                        && interaction.containsKey(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID);
            }
        });
    }

    public InteractionListenerIndexing(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds,
                                       Predicate<Map<String, String>> shouldIndex) {
        this.interactionsWithUnresolvedOccurrenceIds = interactionsWithUnresolvedOccurrenceIds;
        this.shouldIndex = shouldIndex;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        if (shouldIndex.test(interaction)) {
            String value = interaction.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID);

            if (StringUtils.startsWith(value, "http://arctos.database.museum/guid/")) {
                String[] splitValue = StringUtils.split(value, "?");
                value = splitValue.length == 1 ? value : splitValue[0];
            }
            interactionsWithUnresolvedOccurrenceIds.put(
                    Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, value),
                    new HashMap<>(interaction));
        }
    }
}
