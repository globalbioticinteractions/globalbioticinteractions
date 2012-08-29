package org.trophic.graph.service;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class ITISService extends BaseService implements LSIDLookupService {


    @Override
    public String lookupLSIDByTaxonName(String taxonName) throws LSIDLookupServiceException {

        String lsid = null;
        URI uri = null;
        try {
            uri = new URI("http", null, "www.itis.gov", 80, "/ITISWebService/services/ITISService/searchByScientificName", "srchKey=" + taxonName, null);
        } catch (URISyntaxException e) {
            throw new LSIDLookupServiceException("failed to create uri", e);
        }
        HttpGet get = new HttpGet(uri);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        HttpClient httpClient = new DefaultHttpClient();
        String response = null;
        try {
            response = httpClient.execute(get, responseHandler);
        } catch (IOException e) {
            throw new LSIDLookupServiceException("failed to execute query to [ " + uri.toString() + "]", e);
        }
        boolean isValid = response.contains("<ax21:combinedName>" + taxonName + "</ax21:combinedName>");
        if (isValid) {
            String[] split = response.split("<ax21:tsn>");
            if (split.length > 1) {
                String[] anotherSplit = split[1].split("</ax21:tsn>");
                if (split.length > 1) {
                    lsid = "urn:lsid:itis.gov:itis_tsn:" + anotherSplit[0].trim();
                }
            }
        }
        return lsid;
    }
}
