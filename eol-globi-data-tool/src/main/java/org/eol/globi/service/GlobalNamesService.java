package org.eol.globi.service;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class GlobalNamesService extends BaseHttpClientService implements TaxonPropertyLookupService {


    private final GlobalNamesSources source;

    public GlobalNamesService() {
        this(GlobalNamesSources.ITIS);
    }

    public GlobalNamesService(GlobalNamesSources source) {
        super();
        this.source = source;
    }

    @Override
    public void lookupPropertiesByName(String name, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        HttpClient httpClient = HttpUtil.createHttpClient();
        try {
            URI uri = new URI("http", "resolver.globalnames.org", "/name_resolvers.json", "names=" + name + "&data_source_ids=" + source.getId(), null);
            String result = httpClient.execute(new HttpGet(uri), new BasicResponseHandler());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(result);
            JsonNode dataNode = jsonNode.get("data");
            if (dataNode != null && dataNode.isArray() && dataNode.size() > 0) {
                JsonNode results = dataNode.get(0).get("results");
                if (results != null && results.isArray() && results.size() > 0) {
                    JsonNode firstResult = results.get(0);
                    properties.put(PropertyAndValueDictionary.EXTERNAL_ID, source.getProvider().getIdPrefix() + firstResult.get("taxon_id").getValueAsText());
                    properties.put(PropertyAndValueDictionary.NAME, firstResult.get("canonical_form").getValueAsText());
                    properties.put(PropertyAndValueDictionary.PATH, firstResult.get("classification_path").getValueAsText());
                    String[] ranks = firstResult.get("classification_path_ranks").getValueAsText().split("\\|");
                    properties.put(PropertyAndValueDictionary.RANK, ranks[ranks.length - 1]);
                }
            }
        } catch (URISyntaxException e) {
            throw new TaxonPropertyLookupServiceException("Failed to query", e);
        } catch (ClientProtocolException e) {
            throw new TaxonPropertyLookupServiceException("Failed to query", e);
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("Failed to query", e);
        }
    }
}
