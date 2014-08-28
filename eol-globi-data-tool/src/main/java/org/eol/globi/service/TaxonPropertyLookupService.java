package org.eol.globi.service;

import java.util.Map;

public interface TaxonPropertyLookupService {
    void lookupProperties(Map<String, String> properties) throws TaxonPropertyLookupServiceException;

    void shutdown();
}
