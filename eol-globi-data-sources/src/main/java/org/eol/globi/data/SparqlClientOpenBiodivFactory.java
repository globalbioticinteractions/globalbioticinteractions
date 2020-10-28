package org.eol.globi.data;

import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.util.OpenBiodivClientImpl;
import org.globalbioticinteractions.util.SparqlClientImpl;
import org.globalbioticinteractions.util.SparqlClient;

public class SparqlClientOpenBiodivFactory implements SparqlClientFactory {

    @Override
    public SparqlClient create(ResourceService resourceService) {
        return new OpenBiodivClientImpl(resourceService);
    }

}
