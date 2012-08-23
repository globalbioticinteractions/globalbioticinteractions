package org.trophic.graph.service;

public interface LSIDLookupService {
    String lookupLSIDByTaxonName(String taxonName) throws LSIDLookupServiceException;

    void shutdown();
}
