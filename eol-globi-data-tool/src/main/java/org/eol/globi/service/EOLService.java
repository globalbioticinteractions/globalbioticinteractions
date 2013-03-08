package org.eol.globi.service;

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
        String pageId;

        try {
            URI uri = new URI("http", null, "eol.org", 80, "/api/search/1.0/" + taxonName, "exact=true", null);
            pageId = getPageId(taxonName, uri, true);
        } catch (URISyntaxException e) {
            throw new LSIDLookupServiceException("failed to create uri", e);
        }

        return pageId == null ? null : EOLTaxonImageService.EOL_LSID_PREFIX + pageId;
    }

    private String getPageId(String taxonName, URI uri, boolean shouldFollowAlternate) throws LSIDLookupServiceException, URISyntaxException {
        HttpGet get = new HttpGet(uri);

        BasicResponseHandler responseHandler = new BasicResponseHandler();
        HttpClient httpClient = new DefaultHttpClient();
        String response = null;
        try {
            response = httpClient.execute(get, responseHandler);
            System.out.println(response);
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
        if (response != null) {
            if (response.contains("totalResults>1<")) {
                String[] strings = response.split("<entry>");
                if (strings.length > 1) {
                    String[] anotherSplit = strings[1].split("<id>");
                    if (anotherSplit.length > 1) {
                        String[] yetAnotherSplit = anotherSplit[1].split("</id>");
                        pageId = yetAnotherSplit.length > 1 ? yetAnotherSplit[0].trim() : null;
                    }
                }
            } else if (shouldFollowAlternate && response.contains("totalResults>0<")) {
                String[] alternates = response.split("<link rel=\"alternate\" href=\"");
                if (alternates.length > 1) {
                    String[] urlSplit = alternates[1].split("\"");
                    if (urlSplit.length > 1) {
                        String alternateUrlString = urlSplit[0];
                        URI alternateUri = new URI(alternateUrlString);
                        pageId = getPageId(taxonName, alternateUri, false);
                    }

                }

            }
        }
        return pageId;
    }

    @Override
    public void shutdown() {

    }
}
