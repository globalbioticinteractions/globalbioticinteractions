package org.eol.globi.data;

import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;

public class OccurrenceIdIdEnricherGenBankIT extends OccurrenceIdIdEnricherGenBankTest {

    @Override
    public ResourceService getResourceService() {
        return new ResourceServiceHTTP(new InputStreamFactoryNoop());
    }

}