package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class ITISService extends BasePropertyEnricherService {

    @Override
    public String lookupIdByName(String taxonName) throws PropertyEnricherException {

        String response = getResponse("searchByScientificName", "srchKey=" + taxonName);
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

    private String getResponse(String methodName, String queryString) throws PropertyEnricherException {
        URI uri;
        try {
            uri = new URI("http", null, "www.itis.gov", 80, "/ITISWebService/services/ITISService/" + methodName, queryString, null);
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        }
        HttpGet get = new HttpGet(uri);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = execute(get, responseHandler);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to execute query to [ " + uri.toString() + "]", e);
        }
        return response;
    }

    @Override
    public String lookupTaxonPathById(String id) throws PropertyEnricherException {
        String result = null;
        if (StringUtils.isNotBlank(id) && id.startsWith(TaxonomyProvider.ID_PREFIX_ITIS)) {
            String tsn = id.replace(TaxonomyProvider.ID_PREFIX_ITIS, "");
            String fullHierarchy = getResponse("getFullHierarchyFromTSN", "tsn=" + tsn);
            ServiceUtil.extractPath(fullHierarchy, "taxonName");
        }

        return result;
    }

}
