package org.eol.globi.service;

public interface LSIDLookupService {
    String lookupExternalTaxonIdByName(String taxonName) throws LSIDLookupServiceException;

    void shutdown();
}
