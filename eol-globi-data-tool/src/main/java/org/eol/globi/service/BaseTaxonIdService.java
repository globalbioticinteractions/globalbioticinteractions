package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;

import java.util.Map;

public abstract class BaseTaxonIdService extends BaseHttpClientService implements TaxonPropertyLookupService {

    protected String lookupPropertyValueByTaxonName(String taxonName, String propertyName) throws TaxonPropertyLookupServiceException {
        String propertyValue = null;
        if (PropertyAndValueDictionary.EXTERNAL_ID.equals(propertyName)) {
            if (StringUtils.length(taxonName) > 2) {
                try {
                    propertyValue = lookupIdByName(taxonName);
                } catch (TaxonPropertyLookupServiceException e) {
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
            } catch (TaxonPropertyLookupServiceException e) {
                shutdown();
                throw e;
            }
        }
        return propertyValue;
    }

    @Override
    public void lookupPropertiesByName(String name, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        for (String propertyName : properties.keySet()) {
            String propertyValue = lookupPropertyValueByTaxonName(name, propertyName);
            if (propertyValue != null) {
                properties.put(propertyName, propertyValue);
            }
        }
    }

    public abstract String lookupIdByName(String taxonName) throws TaxonPropertyLookupServiceException;

    public abstract String lookupTaxonPathById(String id) throws TaxonPropertyLookupServiceException;

}
