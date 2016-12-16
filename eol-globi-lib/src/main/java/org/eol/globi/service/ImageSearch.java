package org.eol.globi.service;

import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;

public interface ImageSearch {
    TaxonImage lookupImageForExternalId(String externalId) throws IOException;
}
