package org.eol.globi.server;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface TaxonSearch {
    Map<String,String> findTaxon(String scientificName) throws IOException;
    Map<String,String> findTaxonWithImage(String scientificName) throws IOException;
    Collection<String> findTaxonIds(String scientificName) throws IOException;
}
