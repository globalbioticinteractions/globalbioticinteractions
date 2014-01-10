package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;

import java.util.Map;

public class NoMatchService implements TaxonPropertyLookupService {

    @Override
    public void lookupPropertiesByName(String name, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        if (properties.containsKey(Taxon.EXTERNAL_ID)) {
            String value = properties.get(Taxon.EXTERNAL_ID);
            if (StringUtils.isBlank(value)) {
                properties.put(Taxon.EXTERNAL_ID, PropertyAndValueDictionary.NO_MATCH);
            }
        }
    }

    @Override
    public void shutdown() {

    }

}
