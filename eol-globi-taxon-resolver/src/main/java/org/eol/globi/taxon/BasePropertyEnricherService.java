package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class BasePropertyEnricherService implements PropertyEnricher {

    protected String lookupPropertyValueByTaxonName(String taxonName, String propertyName) throws PropertyEnricherException {
        String propertyValue = null;
        if (StringUtils.length(taxonName) > 2) {
            if (PropertyAndValueDictionary.EXTERNAL_ID.equals(propertyName)) {
                try {
                    propertyValue = lookupIdByName(taxonName);
                } catch (PropertyEnricherException e) {

                    throw e;
                }
            } else if (PropertyAndValueDictionary.PATH.equals(propertyName)) {
                try {
                    String lsId = lookupIdByName(taxonName);
                    if (StringUtils.isNotBlank(lsId)) {
                        propertyValue = lookupTaxonPathById(lsId);
                    }
                } catch (PropertyEnricherException e) {

                    throw e;
                }

            }
        }
        return propertyValue;
    }

    @Override
    public Map<String, String> enrich(final Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        ;
        for (String propertyName : enrichedProperties.keySet()) {
            String propertyValue = lookupPropertyValueByTaxonName(enrichedProperties.get(PropertyAndValueDictionary.NAME), propertyName);
            if (propertyValue != null) {
                enrichedProperties.put(propertyName, propertyValue);
            }
        }
        return Collections.unmodifiableMap(enrichedProperties);
    }

    public abstract String lookupIdByName(String taxonName) throws PropertyEnricherException;

    public abstract String lookupTaxonPathById(String id) throws PropertyEnricherException;

    public void shutdown() {

    }
}
