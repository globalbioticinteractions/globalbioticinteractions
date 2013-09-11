package org.eol.globi.service;

import java.util.Map;

public interface TaxonPropertyLookupService {
    void lookupPropertiesByName(String taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException;

    void shutdown();
}
