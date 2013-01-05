package org.trophic.graph.service;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class EOLService extends BaseService implements LSIDLookupService {

    @Override
    public String lookupLSIDByTaxonName(String taxonName) throws LSIDLookupServiceException {
        URI uri = null;
        try {
            uri = new URI("http", null, "eol.org", 80, "/api/search/1.0/" + taxonName, "exact=true", null);
        } catch (URISyntaxException e) {
            throw new LSIDLookupServiceException("failed to create uri", e);
        }
        HttpGet get = new HttpGet(uri);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        HttpClient httpClient = new DefaultHttpClient();
        String response = null;
        try {
            response = httpClient.execute(get, responseHandler);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != 406 && e.getStatusCode() != 404) {
                throw new LSIDLookupServiceException("failed to lookup [" + taxonName + "]", e);
            }
        } catch (ClientProtocolException e) {
            throw new LSIDLookupServiceException("failed to lookup [" + taxonName + "]", e);
        } catch (IOException e) {
            throw new LSIDLookupServiceException("failed to lookup [" + taxonName + "]", e);
        }


        String pageId = null;
        // only match when there's one and only one result
        if (response != null && response.contains("totalResults>1<")) {
            String[] strings = response.split("<entry>");
            if (strings.length > 1) {
                String[] anotherSplit = strings[1].split("<id>");
                if (anotherSplit.length > 1) {
                    String[] yetAnotherSplit = anotherSplit[1].split("</id>");
                    pageId = yetAnotherSplit.length > 1 ? yetAnotherSplit[0].trim() : null;
                }
            }
        }

        return pageId == null ? null : EOLTaxonImageService.EOL_LSID_PREFIX + pageId;
    }

    @Override
    public void shutdown() {

    }
}
