package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.Taxon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseTaxonIdService extends BaseHttpClientService implements TaxonPropertyLookupService {
    private static final Log LOG = LogFactory.getLog(BaseHttpClientService.class);

    protected String lookupPropertyValueByTaxonName(String taxonName, String propertyName) throws TaxonPropertyLookupServiceException {
        String propertyValue = null;
        if (NodeBacked.EXTERNAL_ID.equals(propertyName)) {
            if (taxonName.trim().length() < 2) {
                LOG.warn("taxon name [" + taxonName + "] too short");
            } else {
                try {
                    propertyValue = lookupIdByName(taxonName);
                } catch (TaxonPropertyLookupServiceException e) {
                    shutdown();
                    throw e;
                }
            }
        } else if (Taxon.PATH.equals(propertyName)) {
            try {
                String lsId = lookupIdByName(taxonName);
                if (lsId != null) {
                    propertyValue = lookupTaxonPathByLSID(lsId);
                    // append synonyms in path whenever available using "|" separator with suffix to enable search
                    // see https://github.com/jhpoelen/eol-globi-data/issues/12
                    if (!StringUtils.endsWith(propertyValue, taxonName)) {
                        propertyValue += " | " + taxonName;
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
    public void lookupPropertiesByName(String taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        for (String propertyName : properties.keySet()) {
            String propertyValue = lookupPropertyValueByTaxonName(taxonName, propertyName);
            if (propertyValue != null) {
                properties.put(propertyName, propertyValue);
            }
        }
    }


    @Override
    public boolean canLookupProperty(String propertyName) {
        return NodeBacked.EXTERNAL_ID.equals(propertyName);
    }

    public abstract String lookupIdByName(String taxonName) throws TaxonPropertyLookupServiceException;

    public abstract String lookupTaxonPathByLSID(String lsid) throws TaxonPropertyLookupServiceException;

}
