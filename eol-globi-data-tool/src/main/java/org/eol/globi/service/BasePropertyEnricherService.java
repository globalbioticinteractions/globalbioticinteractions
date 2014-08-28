package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;

import java.util.Map;

public abstract class BasePropertyEnricherService extends BaseHttpClientService implements PropertyEnricher {

    protected String lookupPropertyValueByTaxonName(String taxonName, String propertyName) throws PropertyEnricherException {
        String propertyValue = null;
        if (PropertyAndValueDictionary.EXTERNAL_ID.equals(propertyName)) {
            if (StringUtils.length(taxonName) > 2) {
                try {
                    propertyValue = lookupIdByName(taxonName);
                } catch (PropertyEnricherException e) {
                    shutdown();
                    throw e;
                }
            }
        } else if (PropertyAndValueDictionary.PATH.equals(propertyName)) {
            try {
                String lsId = lookupIdByName(taxonName);
                if (StringUtils.isNotBlank(lsId)) {
                    propertyValue = lookupTaxonPathById(lsId);
                }
            } catch (PropertyEnricherException e) {
                shutdown();
                throw e;
            }
        }
        return propertyValue;
    }

    @Override
    public void enrich(Map<String, String> properties) throws PropertyEnricherException {
        for (String propertyName : properties.keySet()) {
            String propertyValue = lookupPropertyValueByTaxonName(properties.get(PropertyAndValueDictionary.NAME), propertyName);
            if (propertyValue != null) {
                properties.put(propertyName, propertyValue);
            }
        }
    }

    public abstract String lookupIdByName(String taxonName) throws PropertyEnricherException;

    public abstract String lookupTaxonPathById(String id) throws PropertyEnricherException;

}
