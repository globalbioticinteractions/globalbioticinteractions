package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class OccurrenceIdIdEnricherINaturalistIT extends OccurrenceIdIdEnricherINaturalistTest {

    @Override
    public ResourceService getResourceService() {
        return new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                return IOUtils.toInputStream(HttpUtil.getContent(resourceName), StandardCharsets.UTF_8);
            }
        };
    }

}