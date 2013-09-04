package org.eol.globi.service;

import org.eol.globi.domain.NodeBacked;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;

public class NoMatchService implements TaxonPropertyLookupService {

    @Override
    public String lookupPropertyValueByTaxonName(String taxonName, String propertyName) throws TaxonPropertyLookupServiceException {
        return PropertyAndValueDictionary.NO_MATCH;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean canLookupProperty(String propertyName) {
        return NodeBacked.EXTERNAL_ID.equals(propertyName) || Taxon.PATH.equals(propertyName);
    }
}
