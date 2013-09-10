package org.eol.globi.service;

import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoMatchService implements TaxonPropertyLookupService {

    @Override
    public void lookupPropertiesByName(String taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        for (String propertyName : properties.keySet()) {
            if (canLookupProperty(propertyName) && properties.get(propertyName) == null) {
                properties.put(propertyName, PropertyAndValueDictionary.NO_MATCH);
            }
        }
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean canLookupProperty(String propertyName) {
        return NodeBacked.EXTERNAL_ID.equals(propertyName) || Taxon.PATH.equals(propertyName);
    }
}
