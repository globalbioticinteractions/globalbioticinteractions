package org.eol.globi.data;

import org.eol.globi.domain.LogContext;
import org.eol.globi.process.InteractionListener;
import org.eol.globi.process.InteractionValidator;
import org.eol.globi.tool.NullImportLogger;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.globalbioticinteractions.dataset.DatasetWithResourceMapping;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class DatasetImporterForBatBaseIT {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void importAll() throws StudyImporterException, IOException {
        AtomicInteger counter = new AtomicInteger(0);
        DatasetImporterForBatBase importer = new DatasetImporterForBatBase(null, null);
        ResourceServiceLocalAndRemote resourceService = new ResourceServiceLocalAndRemote(new InputStreamFactoryNoop(), folder.newFolder());
        DatasetImpl dataset = new DatasetWithResourceMapping("test/batplant", URI.create("classpath:/org/eol/globi/data/batplant/"), resourceService);
        importer.setDataset(dataset);
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