package org.globalbioticinteractions.dataset;

import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ResourceServiceWithMapping implements ResourceService {
    private final Dataset dataset;
    private ResourceService resourceService;

    public ResourceServiceWithMapping(Dataset dataset, ResourceService resourceService) {
        this.dataset = dataset;
        this.resourceService = resourceService;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        URI absoluteResourceURI = DatasetUtil.mapResourceForDataset(dataset, resourceName);
        return resourceService.retrieve(absoluteResourceURI);
    }

}
