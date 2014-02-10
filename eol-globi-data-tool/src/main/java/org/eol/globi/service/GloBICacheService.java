package org.eol.globi.service;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;

public class GloBICacheService implements TaxonPropertyLookupService {
    @Override
    public void lookupPropertiesByName(String name, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        HttpClient httpClient = HttpUtil.createHttpClient();
        try {
            URI uri = new URI("http", null, "www.trophicgraph.com", 8080, "/findTaxon/" + name, null, null);
            String response = httpClient.execute(new HttpGet(uri), new BasicResponseHandler());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            if (node != null) {
                Iterator<String> fieldNames = node.getFieldNames();
                while(fieldNames.hasNext()) {
                    String key = fieldNames.next();
                    properties.put(key, node.get(key).getTextValue());
                }
            }
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("failed to retrieve information about [" + name + "]", e);
        } catch (URISyntaxException e) {
            throw new TaxonPropertyLookupServiceException("failed to retrieve information about [" + name + "]", e);
        }
    }

    @Override
    public void shutdown() {

    }
}
