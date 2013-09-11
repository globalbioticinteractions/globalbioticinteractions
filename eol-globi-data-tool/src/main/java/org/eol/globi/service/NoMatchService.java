package org.eol.globi.service;

import org.eol.globi.domain.PropertyAndValueDictionary;

import java.util.Map;

public class NoMatchService implements TaxonPropertyLookupService {

    @Override
    public void lookupPropertiesByName(String taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        for (String propertyName : properties.keySet()) {
            if (properties.get(propertyName) == null) {
                properties.put(propertyName, PropertyAndValueDictionary.NO_MATCH);
            }
        }
    }

    @Override
    public void shutdown() {

    }

}
