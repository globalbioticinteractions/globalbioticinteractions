package org.eol.globi.taxon;

import java.io.IOException;

public interface TaxonLookupService {
    org.eol.globi.domain.Taxon[] lookupTermsByName(String taxonName) throws IOException;

    org.eol.globi.domain.Taxon[] lookupTermsById(String taxonId) throws IOException;

    void destroy();
}
