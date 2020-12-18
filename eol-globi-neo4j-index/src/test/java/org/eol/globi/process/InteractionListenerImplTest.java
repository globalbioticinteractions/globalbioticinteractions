package org.eol.globi.process;

import org.apache.commons.io.IOUtils;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.mockito.Mockito;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class InteractionListenerImplTest extends GraphDBTestCase {

    @Test
    public void processIncompleteMessage() throws StudyImporterException {
        InteractionListenerImpl interactionListener = new InteractionListenerImpl(
                nodeFactory,
                null,
                null,
                null
        );
        HashMap<String, String> interaction = new HashMap<>();
        interaction.put("ping", "pong");
        assertStudyCount(0L);
        interactionListener.on(interaction);
        assertStudyCount(0L);
    }

    @Test
    public void processCompleteMessage() throws StudyImporterException {
        InteractionListenerImpl interactionListener = new InteractionListenerImpl(
                nodeFactory,
                null,
                null,
                null);
        HashMap<String, String> interaction = new HashMap<>();
        interaction.put(SOURCE_TAXON_NAME, "sourceName");
        interaction.put(DatasetImporterForTSV.INTERACTION_TYPE_ID, InteractType.INTERACTS_WITH.getIRI());
        interaction.put(TARGET_TAXON_NAME, "targetName");
        interaction.put(REFERENCE_ID, "citation");

        assertStudyCount(0L);

        interactionListener.on(interaction);

        assertStudyCount(1L);

    }

    private String getTestMap() {
        return "provided_interaction_type_label,provided_interaction_type_id,mapped_to_interaction_type_label,mapped_to_interaction_type_id\n" +
                "shouldBeMapped,,interactsWith, http://purl.obolibrary.org/obo/RO_0002437";
    }


    @Test
    public void processMessageWithTranslatedInteractionType() throws StudyImporterException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("provided_interaction_type_id\nshouldBeIgnored", StandardCharsets.UTF_8))
                .thenReturn(IOUtils.toInputStream("provided_interaction_type_id\nshouldBeIgnored", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(getTestMap(), StandardCharsets.UTF_8));

        Dataset dataset = new DatasetImpl("bla", URI.create("foo:bar"), resourceService);


        InteractionListenerImpl interactionListener = new InteractionListenerImpl(
                nodeFactory,
                null,
                null,
                dataset);
        HashMap<String, String> interaction = new HashMap<>();
        interaction.put(SOURCE_TAXON_NAME, "sourceName");
        interaction.put(DatasetImporterForTSV.INTERACTION_TYPE_NAME, "shouldBeMapped");
        interaction.put(TARGET_TAXON_NAME, "targetName");
        interaction.put(REFERENCE_ID, "citation");

        assertStudyCount(0L);

        interactionListener.on(interaction);

        assertStudyCount(1L);

    }

    @Test
    public void processMessageWithOIgnoredInteractionType() throws StudyImporterException, IOException {
        ResourceService resourceService = Mockito.mock(ResourceService.class);
        when(resourceService.retrieve(URI.create("interaction_types_ignored.csv")))
                .thenReturn(IOUtils.toInputStream("provided_interaction_type_id\nshouldBeIgnored", StandardCharsets.UTF_8))
                .thenReturn(IOUtils.toInputStream("provided_interaction_type_id\nshouldBeIgnored", StandardCharsets.UTF_8));
        when(resourceService.retrieve(URI.create("interaction_types_mapping.csv")))
                .thenReturn(IOUtils.toInputStream(getTestMap(), StandardCharsets.UTF_8));

        Dataset dataset = new DatasetImpl("bla", URI.create("foo:bar"), resourceService);


        InteractionListenerImpl interactionListener = new InteractionListenerImpl(
                nodeFactory,
                null,
                null,
                dataset);
        HashMap<String, String> interaction = new HashMap<>();
        interaction.put(SOURCE_TAXON_NAME, "sourceName");
        interaction.put(DatasetImporterForTSV.INTERACTION_TYPE_NAME, "shouldBeIgnored");
        interaction.put(TARGET_TAXON_NAME, "targetName");
        interaction.put(REFERENCE_ID, "citation");

        assertStudyCount(0L);

        interactionListener.on(interaction);

        assertStudyCount(0L);

    }

    public void assertStudyCount(long expectedStudyCount) {
        Result execute = getGraphDb()
                .execute(
                        "START study = node:studies('*:*') " +
                                "RETURN count(study) as study_count"
                );

        assertThat(execute.next().get("study_count"), Is.is(expectedStudyCount));
    }

}
