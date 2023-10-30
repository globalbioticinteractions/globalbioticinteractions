package org.eol.globi.util;

import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.TaxonUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertNotNull;

public class InteractionListenerIndexingTest {

    @Test
    public void extractCatalogNumber() {
        List<String> s = InteractionListenerIndexing
                .inferOccurrenceId("USNPC # 081321");
        assertThat(s, hasItem("United States National Parasite Collection USNPC 81321"));
        assertThat(s, hasItem("United States National Parasite Collection USNPC 081321"));
        assertThat(s, hasItem("United States National Parasite Collection 081321"));
        assertThat(s, hasItem("United States National Parasite Collection 81321"));
    }

    @Test
    public void extractCatalogNumberUSNPC() {
        List<String> s = InteractionListenerIndexing
                .inferOccurrenceId("USNPC # 81321");
        assertThat(s, hasItem("United States National Parasite Collection USNPC 81321"));
        assertThat(s, hasItem("United States National Parasite Collection USNPC 081321"));
        assertThat(s, hasItem("United States National Parasite Collection 081321"));
        assertThat(s, hasItem("United States National Parasite Collection 81321"));
    }

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
    public void indexOnTargetOcccurenceIdSourceCatalogNumberIdPairs() throws StudyImporterException {
        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        String targetOccurrenceId = "United States National Parasite Collection 81321";
        interactionsWithUnresolvedOccurrenceIds.put(
                Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, targetOccurrenceId), Collections.emptyMap());

        InteractionListener listener = new InteractionListenerIndexing(interactionsWithUnresolvedOccurrenceIds);

        listener.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, "http://n2t.net/ark:/65665/3326e7c2d-d8fa-4e5d-b01f-20d6f4150356");
            put(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER, "USNPC # 081321");
            put(TaxonUtil.SOURCE_TAXON_NAME, "sourceName123");
        }});

        assertThat(interactionsWithUnresolvedOccurrenceIds.size(), Is.is(1));

        Map<String, String> props = interactionsWithUnresolvedOccurrenceIds.get(
                Pair.of(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, targetOccurrenceId));

        assertNotNull(props);
        assertThat(props.get(DatasetImporterForTSV.TARGET_OCCURRENCE_ID), Is.is("http://n2t.net/ark:/65665/3326e7c2d-d8fa-4e5d-b01f-20d6f4150356"));
        assertThat(props.get(DatasetImporterForTSV.TARGET_CATALOG_NUMBER), Is.is("USNPC # 081321"));
        assertThat(props.get(TaxonUtil.TARGET_TAXON_NAME), Is.is("sourceName123"));
    }

    @Test
    public void indexOnTargetTaxonIdSourceTaxonIdPairs() throws StudyImporterException {
        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        String targetTaxonId = "taxon:123";
        interactionsWithUnresolvedOccurrenceIds.put(
                Pair.of(TaxonUtil.TARGET_TAXON_ID, targetTaxonId),
                Collections.emptyMap()
        );

        InteractionListener listener = new InteractionListenerIndexing(interactionsWithUnresolvedOccurrenceIds);

        listener.on(new TreeMap<String, String>() {{
            put(TaxonUtil.SOURCE_TAXON_ID, "taxon:123");
            put(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER, "USNPC # 081321");
            put(TaxonUtil.SOURCE_TAXON_NAME, "sourceName123");
        }});

        assertThat(interactionsWithUnresolvedOccurrenceIds.size(), Is.is(1));

        Map<String, String> props = interactionsWithUnresolvedOccurrenceIds.get(
                Pair.of(TaxonUtil.TARGET_TAXON_ID, targetTaxonId));

        assertNotNull(props);
        assertThat(props.get(TaxonUtil.TARGET_TAXON_ID), Is.is("taxon:123"));
        assertThat(props.get(TaxonUtil.TARGET_TAXON_NAME), Is.is("sourceName123"));
    }

    @Test
    public void indexOnSourceTaxonIdTargetTaxonIdPairs() throws StudyImporterException {
        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        String targetTaxonId = "taxon:123";
        interactionsWithUnresolvedOccurrenceIds.put(
                Pair.of(TaxonUtil.SOURCE_TAXON_ID, targetTaxonId),
                Collections.emptyMap()
        );

        InteractionListener listener = new InteractionListenerIndexing(interactionsWithUnresolvedOccurrenceIds);

        listener.on(new TreeMap<String, String>() {{
            put(TaxonUtil.TARGET_TAXON_ID, "taxon:123");
            put(DatasetImporterForTSV.TARGET_CATALOG_NUMBER, "USNPC # 081321");
            put(TaxonUtil.TARGET_TAXON_NAME, "sourceName123");
        }});

        assertThat(interactionsWithUnresolvedOccurrenceIds.size(), Is.is(1));

        Map<String, String> props = interactionsWithUnresolvedOccurrenceIds.get(
                Pair.of(TaxonUtil.SOURCE_TAXON_ID, targetTaxonId));

        assertNotNull(props);
        assertThat(props.get(TaxonUtil.SOURCE_TAXON_ID), Is.is("taxon:123"));
        assertThat(props.get(TaxonUtil.SOURCE_TAXON_NAME), Is.is("sourceName123"));
    }

    @Test
    public void indexOnSourceOcccurenceIdTargetCatalogNumberIdPairs() throws StudyImporterException {
        TreeMap<Pair<String, String>, Map<String, String>> interactionsWithUnresolvedOccurrenceIds = new TreeMap<>();
        String targetOccurrenceId = "United States National Parasite Collection 81321";
        interactionsWithUnresolvedOccurrenceIds.put(Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, targetOccurrenceId), Collections.emptyMap());

        InteractionListener listener = new InteractionListenerIndexing(interactionsWithUnresolvedOccurrenceIds);

        listener.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.TARGET_OCCURRENCE_ID, "http://n2t.net/ark:/65665/3326e7c2d-d8fa-4e5d-b01f-20d6f4150356");
            put(DatasetImporterForTSV.TARGET_CATALOG_NUMBER, "USNPC # 081321");
            put(TaxonUtil.TARGET_TAXON_NAME, "sourceName123");
        }});

        assertThat(interactionsWithUnresolvedOccurrenceIds.size(), Is.is(1));

        Map<String, String> props = interactionsWithUnresolvedOccurrenceIds.get(
                Pair.of(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID, targetOccurrenceId));

        assertNotNull(props);
        assertThat(props.get(DatasetImporterForTSV.SOURCE_OCCURRENCE_ID), Is.is("http://n2t.net/ark:/65665/3326e7c2d-d8fa-4e5d-b01f-20d6f4150356"));
        assertThat(props.get(DatasetImporterForTSV.SOURCE_CATALOG_NUMBER), Is.is("USNPC # 081321"));
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