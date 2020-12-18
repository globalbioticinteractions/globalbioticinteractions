package org.eol.globi.process;

import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.GraphDBTestCase;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.InteractType;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.neo4j.graphdb.Result;

import java.util.HashMap;

import static org.eol.globi.data.DatasetImporterForTSV.REFERENCE_ID;
import static org.eol.globi.service.TaxonUtil.SOURCE_TAXON_NAME;
import static org.eol.globi.service.TaxonUtil.TARGET_TAXON_NAME;
import static org.hamcrest.MatcherAssert.assertThat;

public class InteractionListenerImplTest extends GraphDBTestCase {

    @Test
    public void processIncompleteMessage() throws StudyImporterException {
        InteractionListenerImpl interactionListener = new InteractionListenerImpl(
                nodeFactory,
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

    public void assertStudyCount(long expectedStudyCount) {
        Result execute = getGraphDb()
                .execute(
                        "START study = node:studies('*:*') " +
                                "RETURN count(study) as study_count"
                );

        assertThat(execute.next().get("study_count"), Is.is(expectedStudyCount));
    }

}
