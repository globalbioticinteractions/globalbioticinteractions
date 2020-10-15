package org.globalbioticinteractions.util;

import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.ResourceService;

public class OpenBiodivClientImpl extends SparqlClientImpl {

    public OpenBiodivClientImpl(ResourceService resourceService) {
        super(resourceService, PropertyAndValueDictionary.SPARQL_ENDPOINT_OPEN_BIODIV);
    }
}
