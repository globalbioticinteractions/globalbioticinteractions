package org.eol.globi.process;

import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LogContext;
import org.eol.globi.tool.NullImportLogger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.data.DatasetImporterForTSV.DATASET_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_CITATION;
import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_CATALOG_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_COLLECTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_COLLECTION_ID;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_INSTITUTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.SOURCE_OCCURRENCE_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_CATALOG_NUMBER;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_COLLECTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_COLLECTION_ID;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_INSTITUTION_CODE;
import static org.eol.globi.data.DatasetImporterForTSV.TARGET_OCCURRENCE_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_PATH_NAMES;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_SPECIFIC_EPITHET;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;

public class InteractionExpanderTest {

    @Test
    public void importWithMissingTargetTaxonButAvailableInstitutionCollectionCatalogTriple() throws StudyImporterException {
        List<String> msgs = new ArrayList<>();
        List<Map<String, String>> received = new ArrayList<>();
        final InteractionListener listener = new InteractionExpander(
                interaction -> received.add(interaction),
                new NullImportLogger() {
                    @Override
                    public void info(LogContext ctx, String message) {
                        msgs.add(message);
                    }

                    @Override
                    public void warn(LogContext ctx, String message) {
                        msgs.add(message);
                    }

                    @Override
                    public void severe(LogContext ctx, String message) {
                        msgs.add(message);
                    }
                });
        final TreeMap<String, String> link = new TreeMap<>();
        link.put(SOURCE_TAXON_NAME, "Donald duck");
        link.put(SOURCE_TAXON_PATH, "Aves | Donald | Donald duck");
        link.put(SOURCE_TAXON_PATH_NAMES, "class | genus | species");
        link.put(SOURCE_TAXON_SPECIFIC_EPITHET, "duck");
        link.put(DatasetImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
        link.put(TARGET_OCCURRENCE_ID, "occurrenceId123");
        link.put(TARGET_INSTITUTION_CODE, "institutionCode123");
        link.put(TARGET_COLLECTION_CODE, "collectionCode123");
        link.put(TARGET_COLLECTION_ID, "collectionId123");
        link.put(TARGET_CATALOG_NUMBER, "catalogNumber123");
        link.put(DATASET_CITATION, "some source ref");
        link.put(REFERENCE_ID, "123");
        link.put(REFERENCE_CITATION, "");

        listener.on(link);
        assertThat(msgs,
                hasItem("target taxon name missing: using institutionCode/collectionCode/collectionId/catalogNumber/occurrenceId as placeholder"));

        assertThat(received.size(), is(1));

        Map<String, String> receivedInteraction = received.get(0);
        assertThat(receivedInteraction.get(TARGET_TAXON_NAME),
                is("institutionCode123 | collectionCode123 | collectionId123 | catalogNumber123 | occurrenceId123"));

    }

    @Test
    public void importWithMissingSourceTaxonButAvailableInstitutionCollectionCatalogTriple() throws StudyImporterException {
        List<String> msgs = new ArrayList<>();
        List<Map<String, String>> received = new ArrayList<>();
        final InteractionListener listener = new InteractionExpander(
                interaction -> received.add(interaction),
                new NullImportLogger() {
                    @Override
                    public void info(LogContext ctx, String message) {
                        msgs.add(message);
                    }

                    @Override
                    public void warn(LogContext ctx, String message) {
                        msgs.add(message);
                    }

                    @Override
                    public void severe(LogContext ctx, String message) {
                        msgs.add(message);
                    }
                });
        final TreeMap<String, String> link = new TreeMap<>();
        link.put(TARGET_TAXON_NAME, "Donald duck");
        link.put(DatasetImporterForTSV.INTERACTION_TYPE_ID, InteractType.ATE.getIRI());
        link.put(SOURCE_OCCURRENCE_ID, "occurrenceId123");
        link.put(SOURCE_INSTITUTION_CODE, "institutionCode123");
        link.put(SOURCE_COLLECTION_CODE, "collectionCode123");
        link.put(SOURCE_COLLECTION_ID, "collectionId123");
        link.put(SOURCE_CATALOG_NUMBER, "catalogNumber123");
        link.put(DATASET_CITATION, "some source ref");
        link.put(REFERENCE_ID, "123");
        link.put(REFERENCE_CITATION, "");

        listener.on(link);
        assertThat(msgs, hasItem("source taxon name missing: using institutionCode/collectionCode/collectionId/catalogNumber/occurrenceId as placeholder"));

        assertThat(received.size(), is(1));

        String expectedPlaceholder = "institutionCode123 | collectionCode123 | collectionId123 | catalogNumber123 | occurrenceId123";
        assertThat(received.get(0).get(SOURCE_TAXON_NAME), is(expectedPlaceholder));

    }


}