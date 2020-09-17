package org.eol.globi.data;

import org.eol.globi.domain.LogContext;
import org.eol.globi.tool.NullImportLogger;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DatasetImporterForBatBaseIT {

    @Test
    public void importAll() throws StudyImporterException {
        AtomicInteger counter = new AtomicInteger(0);
        DatasetImporterForBatBase importer = new DatasetImporterForBatBase(null, null);
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