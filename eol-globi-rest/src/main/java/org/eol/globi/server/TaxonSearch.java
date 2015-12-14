package org.eol.globi.server;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

public interface TaxonSearch {
    Map<String,String> findTaxon(String scientificName, HttpServletRequest request) throws IOException;
    Map<String,String> findTaxonWithImage(String scientificName) throws IOException;
}
