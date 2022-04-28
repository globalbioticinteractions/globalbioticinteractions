package org.globalbioticinteractions.dataset;

import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetWithResourceMapping extends DatasetImpl {


    public DatasetWithResourceMapping(String namespace, URI archiveURI, ResourceService resourceService) {
        super(namespace, resourceService, archiveURI);
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        return new ResourceServiceWithMapping(this, getResourceService()).retrieve(resourceName);
    }

}
