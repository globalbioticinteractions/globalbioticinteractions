package org.eol.globi.util;

import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.service.TaxonUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

public class InteractionListenerCollectUnresolvedOccurrenceIdsTest {

    @Test
    public void unresolvedTargetOccurrenceId() throws StudyImporterException {

        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();

        new InteractionListenerCollectUnresolvedOccurrenceIds(interactionsWithUnresolvedOccurrenceIds)
        .on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "source123");
            put(TaxonUtil.SOURCE_TAXON_NAME, "sourceName123");
            put(DatasetImporterForTSV.INTERACTION_TYPE_ID, "foo:bar");
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "target123");
        }});

        assertThat(interactionsWithUnresolvedOccurrenceIds.size(), Is.is(1));

        assertTrue(interactionsWithUnresolvedOccurrenceIds.containsKey(Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "target123")));
    }

    @Test
    public void unresolvedSourceAndTargetOccurrenceId() throws StudyImporterException {

        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds
                = new TreeMap<>();

        new InteractionListenerCollectUnresolvedOccurrenceIds(interactionsWithUnresolvedOccurrenceIds)
        .on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "source123");
            put(DatasetImporterForTSV.INTERACTION_TYPE_ID, "foo:bar");
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "target123");
        }});

        assertThat(interactionsWithUnresolvedOccurrenceIds.size(), Is.is(2));

        assertTrue(interactionsWithUnresolvedOccurrenceIds.containsKey(Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "target123")));
        assertTrue(interactionsWithUnresolvedOccurrenceIds.containsKey(Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "source123")));
    }

}