package org.eol.globi.data;

import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

public class OccurrenceIdIdEnricherGenBankIT extends OccurrenceIdIdEnricherGenBankTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Override
    public ResourceService getResourceService() {
        try {
            return new ResourceServiceHTTP(new InputStreamFactoryNoop(), folder.newFolder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}