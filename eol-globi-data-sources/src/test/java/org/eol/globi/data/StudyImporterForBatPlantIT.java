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