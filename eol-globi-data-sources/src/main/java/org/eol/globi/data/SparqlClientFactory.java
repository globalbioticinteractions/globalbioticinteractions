package org.eol.globi.data;

import org.eol.globi.service.ResourceService;

public interface SparqlClientFactory {
    SparqlClient create(ResourceService resourceService);
}
