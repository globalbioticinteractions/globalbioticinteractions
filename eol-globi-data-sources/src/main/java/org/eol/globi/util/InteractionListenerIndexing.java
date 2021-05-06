package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;

import java.util.HashMap;
import java.util.Map;

public class InteractionListenerIndexing implements InteractionListener {
    private final Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds;

    public InteractionListenerIndexing(Map<String, Map<String, String>> interactionsWithUnresolvedOccurrenceIds) {
        this.interactionsWithUnresolvedOccurrenceIds = interactionsWithUnresolvedOccurrenceIds;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {

        if (interaction.containsKey(DatasetImporterForTSV.TARGET_OCCURRENCE_ID)
                && interaction.containsKey(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID)) {
            String value = interaction.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID);

            if (StringUtils.startsWith(value, "http://arctos.database.museum/guid/")) {
                String[] splitValue = StringUtils.split(value, "?");
                value = splitValue.length == 1 ? value : splitValue[0];
            }
            interactionsWithUnresolvedOccurrenceIds.put(value, new HashMap<>(interaction));
        }
    }
}
