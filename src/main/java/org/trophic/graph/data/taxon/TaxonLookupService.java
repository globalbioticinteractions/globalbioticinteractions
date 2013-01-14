package org.trophic.graph.data.taxon;

import java.io.IOException;

public interface TaxonLookupService {
    long[] lookupTerms(String taxonName) throws IOException;

    void destroy();
}
