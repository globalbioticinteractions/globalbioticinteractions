package org.eol.globi.service;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class ITISService extends BaseExternalIdService  {

    @Override
    public String lookupLSIDByTaxonName(String taxonName) throws TaxonPropertyLookupServiceException {
        URI uri;
        try {
            uri = new URI("http", null, "www.itis.gov", 80, "/ITISWebService/services/ITISService/searchByScientificName", "srchKey=" + taxonName, null);
        } catch (URISyntaxException e) {
            throw new TaxonPropertyLookupServiceException("failed to create uri", e);
        }
        HttpGet get = new HttpGet(uri);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = getHttpClient().execute(get, responseHandler);
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("failed to execute query to [ " + uri.toString() + "]", e);
        }
        String lsid = null;
        boolean isValid = response.contains("<ax21:combinedName>" + taxonName + "</ax21:combinedName>");
        if (isValid) {
            String[] split = response.split("<ax21:tsn>");
            if (split.length > 1) {
                String[] anotherSplit = split[1].split("</ax21:tsn>");
                if (split.length > 1) {
                    lsid = TaxonomyProvider.ID_PREFIX_ITIS + anotherSplit[0].trim();
                }
            }
        }
        return lsid;
    }

    @Override
    public String lookupTaxonPathByLSID(String lsid) throws TaxonPropertyLookupServiceException {
        return null;
    }
}
