package org.eol.globi.data;

import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.util.SparqlClient;

public interface SparqlClientFactory {
    SparqlClient create(ResourceService resourceService);
}
