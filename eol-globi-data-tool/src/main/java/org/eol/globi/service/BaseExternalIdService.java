package org.eol.globi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Taxon;

public abstract class BaseExternalIdService extends BaseHttpClientService implements TaxonPropertyLookupService {
    private static final Log LOG = LogFactory.getLog(BaseHttpClientService.class);

    @Override
    public String lookupPropertyValueByTaxonName(String taxonName, String propertyName) throws TaxonPropertyLookupServiceException {
        String externalId = null;
        if (taxonName.trim().length() < 2) {
            LOG.warn("taxon name [" + taxonName + "] too short");
        } else {
            try {
                externalId = lookupLSIDByTaxonName(taxonName);
            } catch (TaxonPropertyLookupServiceException e) {
                shutdown();

                throw e;
            }
        }
        return externalId;
    }

    @Override
    public boolean canLookupProperty(String propertyName) {
        return Taxon.EXTERNAL_ID.equals(propertyName);
    }

    public abstract String lookupLSIDByTaxonName(String taxonName) throws TaxonPropertyLookupServiceException;


}
