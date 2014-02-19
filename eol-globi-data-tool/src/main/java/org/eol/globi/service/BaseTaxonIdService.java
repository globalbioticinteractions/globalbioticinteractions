package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;

import java.util.Map;

public abstract class BaseTaxonIdService extends BaseHttpClientService implements TaxonPropertyLookupService {
    private static final Log LOG = LogFactory.getLog(BaseHttpClientService.class);

    protected String lookupPropertyValueByTaxonName(String taxonName, String propertyName) throws TaxonPropertyLookupServiceException {
        String propertyValue = null;
        if (PropertyAndValueDictionary.EXTERNAL_ID.equals(propertyName)) {
            if (StringUtils.length(taxonName) < 2) {
                LOG.warn("taxon name [" + taxonName + "] too short");
            } else {
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
                if (lsId != null) {
                    propertyValue = lookupTaxonPathById(lsId);
                    // append synonyms in path whenever available using "|" separator with suffix to enable search
                    // see https://github.com/jhpoelen/eol-globi-data/issues/12
                    if (StringUtils.isNotBlank(propertyValue) && !StringUtils.endsWith(propertyValue, taxonName)) {
                        propertyValue += CharsetConstant.SEPARATOR + taxonName;
                    }
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
