package org.eol.globi.util;

import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TaxonUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;

public class InteractionListenerIndexingTest {

    @Test
    public void indexOnSourceOccurrenceIdTargetOccurrenceIdPairs() throws StudyImporterException {
        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        interactionsWithUnresolvedOccurrenceIds.put(Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "source123"), Collections.emptyMap());

        InteractionListener listener = new InteractionListenerIndexing(interactionsWithUnresolvedOccurrenceIds);

        listener.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "target123");
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "source123");
            put(TaxonUtil.SOURCE_TAXON_NAME, "sourceName123");
        }});

        assertThat(interactionsWithUnresolvedOccurrenceIds.size(), Is.is(1));

        Map<String, String> props = interactionsWithUnresolvedOccurrenceIds.get(
                Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "source123"));

        assertThat(props.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), Is.is("source123"));
        assertThat(props.get(TaxonUtil.SOURCE_TAXON_NAME), Is.is("sourceName123"));
    }

    @Test
    public void indexOnSourceOccurrenceIdTargetOccurrenceIdPairs2() throws StudyImporterException {
        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        interactionsWithUnresolvedOccurrenceIds.put(Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "target123"), Collections.emptyMap());

        InteractionListener listener = new InteractionListenerIndexing(interactionsWithUnresolvedOccurrenceIds);

        listener.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "target123");
            put(TaxonUtil.TARGET_TAXON_NAME, "targetName123");
        }});

        assertThat(interactionsWithUnresolvedOccurrenceIds.size(), Is.is(1));

        Map<String, String> props = interactionsWithUnresolvedOccurrenceIds.get(
                Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "target123"));

        assertThat(props.get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID), Is.is("target123"));
        assertThat(props.get(TaxonUtil.TARGET_TAXON_NAME), Is.is("targetName123"));
    }


    @Test
    public void indexOnSourceOccurrenceIdTargetOccurrenceIdPairs3() throws StudyImporterException {
        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        interactionsWithUnresolvedOccurrenceIds.put(Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "occurrence123"), Collections.emptyMap());

        InteractionListener listener = new InteractionListenerIndexing(interactionsWithUnresolvedOccurrenceIds);

        listener.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "occurrence123");
            put(TaxonUtil.TARGET_TAXON_NAME, "occurrenceName123");
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "occurrence456");
        }});

        assertThat(interactionsWithUnresolvedOccurrenceIds.size(), Is.is(1));

        Map<String, String> props = interactionsWithUnresolvedOccurrenceIds.get(
                Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "occurrence123"));

        assertThat(props.get(TaxonUtil.SOURCE_TAXON_NAME), Is.is("occurrenceName123"));
        assertThat(props.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), Is.is("occurrence123"));

    }

    @Test
    public void indexOnTargetOccurrenceIdOnly() throws StudyImporterException {
        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        InteractionListener listener = new InteractionListenerIndexing(interactionsWithUnresolvedOccurrenceIds);


        listener.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "target123");
            put(TaxonUtil.SOURCE_TAXON_NAME, "sourceName123");
        }});

        assertThat(interactionsWithUnresolvedOccurrenceIds.size(), Is.is(0));
    }

    @Test
    public void indexOnSourceOccurrenceIdOnly() throws StudyImporterException {
        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        InteractionListener listener = new InteractionListenerIndexing(interactionsWithUnresolvedOccurrenceIds);


        listener.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "target123");
            put(TaxonUtil.SOURCE_TAXON_NAME, "sourceName123");
        }});

        assertThat(interactionsWithUnresolvedOccurrenceIds.size(), Is.is(0));
    }

}