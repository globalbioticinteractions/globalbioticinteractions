package org.eol.globi.data.taxon;

import java.io.IOException;

public interface TaxonLookupService {
    TaxonTerm[] lookupTermsByName(String taxonName) throws IOException;

    void destroy();
}
