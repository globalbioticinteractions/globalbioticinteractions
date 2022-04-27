package org.eol.globi.data;

import org.eol.globi.service.ResourceService;
import org.eol.globi.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class GenBankOccurrenceIdIdEnricherIT extends GenBankOccurrenceIdIdEnricherTest {

    @Override
    public ResourceService getResourceService() {
        return new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return ResourceUtil.asInputStream(resourceName, is -> is);
            }
        };
    }

}