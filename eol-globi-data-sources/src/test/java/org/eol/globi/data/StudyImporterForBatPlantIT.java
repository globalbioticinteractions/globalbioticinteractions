package org.eol.globi.data;

import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.LogContext;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.tool.NullImportLogger;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class StudyImporterForBatPlantIT {

    private static final Map<String, InteractType> INTERACTION_TYPE_MAP = new HashMap<String, InteractType>() {{
        // seed dispersal
        put("batplant:interactionTypeId:1", InteractType.DISPERSAL_VECTOR_OF);
        // consume
        put("batplant:interactionTypeId:2", InteractType.ATE);
        // pollination
        put("batplant:interactionTypeId:3", InteractType.POLLINATES);
        // visitation
        put("batplant:interactionTypeId:4", InteractType.VISITS);
        // transports
        put("batplant:interactionTypeId:5", InteractType.DISPERSAL_VECTOR_OF);
        // roosts
        put("batplant:interactionTypeId:6", InteractType.CO_OCCURS_WITH);
        // host
        put("batplant:interactionTypeId:7", InteractType.HOST_OF);
        // cohabitation
        put("batplant:interactionTypeId:8", InteractType.CO_OCCURS_WITH);
        // prey
        put("batplant:interactionTypeId:9", InteractType.PREYED_UPON_BY);
        // Hematophagy (blood eating)
        put("batplant:interactionTypeId:10", InteractType.ATE);
        // Predation
        put("batplant:interactionTypeId:11", InteractType.PREYS_UPON);
    }};


    @Test
    public void importAll() throws StudyImporterException {
        AtomicInteger counter = new AtomicInteger(0);
        StudyImporterForBatPlant importer = new StudyImporterForBatPlant(null, null);
        DatasetImpl dataset = new DatasetImpl("test/batplant", URI.create("classpath:/org/eol/globi/data/batplant/"), is -> is);
        importer.setDataset(dataset);
        importer.setInteractionListener(link -> {

            InteractionListenerImpl.validLink(link, new NullImportLogger() {
                @Override
                public void warn(LogContext ctx, String message) {
                    fail(message + "for [" + link + "]");
                }

            });
            counter.incrementAndGet();
        });

        importer.importStudy();

        assertThat(counter.get() > 0, Is.is(true));
    }


}