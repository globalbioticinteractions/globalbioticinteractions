package org.eol.globi.data.taxon;

import java.io.IOException;

public interface TaxonLookupService {
    long[] lookupTerms(String taxonName) throws IOException;

    void destroy();
}
