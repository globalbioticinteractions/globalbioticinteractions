package org.eol.globi.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class BaseExternalIdService extends BaseService implements LSIDLookupService {
    private static final Log LOG = LogFactory.getLog(BaseService.class);

    public String lookupExternalTaxonIdByName(String taxonName) throws LSIDLookupServiceException {
        String externalId = null;
        if (taxonName.trim().length() < 2) {
            LOG.warn("taxon name [" + taxonName + "] too short");
        } else {
            externalId = lookupLSIDByTaxonName(taxonName);
        }
        return externalId;
    }

    public abstract String lookupLSIDByTaxonName(String taxonName) throws LSIDLookupServiceException;

}
