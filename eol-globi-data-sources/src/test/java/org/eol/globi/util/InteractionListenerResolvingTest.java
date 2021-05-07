package org.eol.globi.util;

import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TaxonUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;

public class InteractionListenerResolvingTest {

    @Test
    public void resolveWithTargetOccurrenceId() throws StudyImporterException {
        List<Map<String, String>> receivedInteractions = new ArrayList<>();

        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        interactionsWithUnresolvedOccurrenceIds.put(
                Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "occurrence123"),
                new TreeMap<String, String>() {{
                    put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "occurrence123");
                    put(TaxonUtil.TARGET_TAXON_NAME, "occurrenceName123");
                }});

        new InteractionListenerResolving(interactionsWithUnresolvedOccurrenceIds, new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                receivedInteractions.add(interaction);
            }
        }).on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "occurrence456");
            put(DatasetImporterForTSV.INTERACTION_TYPE_ID, "foo:bar");
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "occurrence123");
        }});

        assertThat(receivedInteractions.size(), Is.is(1));
        assertThat(receivedInteractions.get(0).get(TaxonUtil.TARGET_TAXON_NAME), Is.is("occurrenceName123"));
        assertThat(receivedInteractions.get(0).get(TaxonUtil.SOURCE_TAXON_NAME), Is.is(nullValue()));
    }

    @Test
    public void resolveWithSourceOccurrenceId() throws StudyImporterException {
        List<Map<String, String>> receivedInteractions = new ArrayList<>();

        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        interactionsWithUnresolvedOccurrenceIds.put(
                Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "occurrence123"),
                new TreeMap<String, String>() {{
                    put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "occurrence123");
                    put(TaxonUtil.SOURCE_TAXON_NAME, "occurrenceName123");
                }});

        new InteractionListenerResolving(interactionsWithUnresolvedOccurrenceIds, new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                receivedInteractions.add(interaction);
            }
        }).on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "occurrence123");
            put(DatasetImporterForTSV.INTERACTION_TYPE_ID, "foo:bar");
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "occurrence456");
        }});

        assertThat(receivedInteractions.size(), Is.is(1));
        assertThat(receivedInteractions.get(0).get(TaxonUtil.SOURCE_TAXON_NAME), Is.is("occurrenceName123"));
        assertThat(receivedInteractions.get(0).get(TaxonUtil.TARGET_TAXON_NAME), Is.is(nullValue()));
    }

    @Test
    public void resolveWithSourceAndTargetOccurrenceId() throws StudyImporterException {
        List<Map<String, String>> receivedInteractions = new ArrayList<>();

        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        interactionsWithUnresolvedOccurrenceIds.put(
                Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "occurrence123"),
                new TreeMap<String, String>() {{
                    put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "occurrence123");
                    put(TaxonUtil.SOURCE_TAXON_NAME, "occurrenceName123");
                }});
        interactionsWithUnresolvedOccurrenceIds.put(
                Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "occurrence456"),
                new TreeMap<String, String>() {{
                    put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "occurrence456");
                    put(TaxonUtil.TARGET_TAXON_NAME, "occurrenceName456");
                }});

        new InteractionListenerResolving(interactionsWithUnresolvedOccurrenceIds, new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                receivedInteractions.add(interaction);
            }
        }).on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "occurrence123");
            put(DatasetImporterForTSV.INTERACTION_TYPE_ID, "foo:bar");
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "occurrence456");
        }});

        assertThat(receivedInteractions.size(), Is.is(1));
        assertThat(receivedInteractions.get(0).get(TaxonUtil.SOURCE_TAXON_NAME), Is.is("occurrenceName123"));
        assertThat(receivedInteractions.get(0).get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), Is.is("occurrence123"));
        assertThat(receivedInteractions.get(0).get(TaxonUtil.TARGET_TAXON_NAME), Is.is("occurrenceName456"));
        assertThat(receivedInteractions.get(0).get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID), Is.is("occurrence456"));
    }

}