package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;

import java.util.Map;

public class NoMatchService implements TaxonPropertyLookupService {

    @Override
    public void lookupPropertiesByName(String taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        for (String propertyName : properties.keySet()) {
            String value = properties.get(propertyName);
            if (StringUtils.isBlank(value)) {
                properties.put(propertyName, PropertyAndValueDictionary.NO_MATCH);
            }
        }
    }

    @Override
    public void shutdown() {

    }

}
