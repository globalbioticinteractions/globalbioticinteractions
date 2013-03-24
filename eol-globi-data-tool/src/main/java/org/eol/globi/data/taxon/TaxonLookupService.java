package org.eol.globi.data.taxon;

import java.io.IOException;

public interface TaxonLookupService {
    String[] lookupTermIds(String taxonName) throws IOException;

    void destroy();
}
