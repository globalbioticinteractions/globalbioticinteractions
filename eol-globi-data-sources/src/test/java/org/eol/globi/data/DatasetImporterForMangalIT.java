package org.eol.globi.data;

import org.eol.globi.domain.LogContext;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionListenerImpl;
import org.eol.globi.process.InteractionValidator;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.tool.NullImportLogger;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class DatasetImporterForMangalIT {

    @Test
    public void importAll() throws StudyImporterException {
        AtomicInteger counter = new AtomicInteger(0);
        DatasetImporterForMangal importer = new DatasetImporterForMangal(null, null);
        importer.setDataset(new DatasetLocal(inStream -> inStream));
        importer.setInteractionListener(new InteractionValidator(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                counter.incrementAndGet();
            }
        }, new NullImportLogger() {
            @Override
            public void warn(LogContext ctx, String message) {
                fail("unexpected warning: [" + message + "]");
            }

        }));

        importer.importStudy();

        assertThat(counter.get() > 0, Is.is(true));
    }

}