package org.eol.globi.service;

public interface TaxonPropertyLookupService {
    String lookupPropertyValueByTaxonName(String taxonName, String propertyName) throws TaxonPropertyLookupServiceException;

    boolean canLookupProperty(String propertyName);

    void shutdown();
}
