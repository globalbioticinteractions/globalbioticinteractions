package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporter;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TaxonUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InteractionListenerCollectUnresolvedOccurrenceIds implements InteractionListener {
    private final Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds;

    public InteractionListenerCollectUnresolvedOccurrenceIds(Map<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds) {
        this.interactionsWithUnresolvedOccurrenceIds = interactionsWithUnresolvedOccurrenceIds;
    }

    @Override
    public void on(Map<String, String> interaction) throws StudyImporterException {
        addUnresolvedSourceOccurrenceId(interaction);
        addUnresolvedTargetOccurrenceId(interaction);
    }

    public void addUnresolvedTargetOccurrenceId(Map<String, String> interaction) {
        if (hasUnresolvedTargetOccurrenceId(interaction)) {
            interactionsWithUnresolvedOccurrenceIds.put(
                    Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID,
                            InteractionListenerIndexing.getOccurrenceId(interaction, DatasetImporterForTSV.TARGET_OCCURRENCE_ID)),
                    Collections.emptyMap());
        }
    }

    public void addUnresolvedSourceOccurrenceId(Map<String, String> interaction) {
        if (hasUnresolvedSourceOccurrenceId(interaction)) {
            interactionsWithUnresolvedOccurrenceIds.put(
                    Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID,
                            InteractionListenerIndexing.getOccurrenceId(interaction, DatasetImporterForTSV.SOURCE_OCCURRENCE_ID)),
                    new HashMap<>(interaction));
        }
    }

    public static boolean hasUnresolvedSourceOccurrenceId(Map<String, String> interaction) {
        return StringUtils.isBlank(StringUtils.defaultString(interaction.get(TaxonUtil.SOURCE_TAXON_NAME),
                TaxonUtil.generateSourceTaxonName(interaction)))
                && interaction.containsKey(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID);
    }

    public static boolean hasUnresolvedTargetOccurrenceId(Map<String, String> interaction) {
        return StringUtils.isBlank(StringUtils.defaultString(interaction.get(TaxonUtil.TARGET_TAXON_NAME),
                TaxonUtil.generateTargetTaxonName(interaction)))
                && interaction.containsKey(DatasetImporterForTSV.TARGET_OCCURRENCE_ID);
    }

    public static boolean hasInteractionTerms(Map<String, String> interaction) {
        return interaction.containsKey(DatasetImporterForTSV.INTERACTION_TYPE_ID)
                || interaction.containsKey(DatasetImporterForTSV.INTERACTION_TYPE_NAME);
    }

}
