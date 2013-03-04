package org.eol.globi.service;

public interface LSIDLookupService {
    String lookupLSIDByTaxonName(String taxonName) throws LSIDLookupServiceException;

    void shutdown();
}
