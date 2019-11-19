package org.eol.globi.service;

import org.eol.globi.domain.TaxonImage;

import java.io.IOException;

public interface ImageSearch {
    TaxonImage lookupImageForExternalId(String externalId) throws IOException;
    TaxonImage lookupImageForExternalId(String externalId, SearchContext context) throws IOException;
}
