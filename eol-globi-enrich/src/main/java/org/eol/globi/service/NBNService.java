package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NBNService implements PropertyEnricher {
    private HttpClient client = null;

    private HttpClient getHttpClient() {
        return client == null ? HttpUtil.createHttpClient() : client;
    }

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        // see https://data.nbn.org.uk/Documentation/Web_Services/Web_Services-REST/Getting_Records/
        Map<String, String> enriched = new HashMap<String, String>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (StringUtils.startsWith(externalId, TaxonomyProvider.NBN.getIdPrefix())) {
            enrichWithExternalId(enriched, externalId);
        }
        return enriched;
    }

    protected void enrichWithExternalId(Map<String, String> enriched, String externalId) throws PropertyEnricherException {
        List<String> ids = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        List<String> ranks = new ArrayList<String>();

        try {
            String nbnId = StringUtils.replace(externalId, TaxonomyProvider.NBN.getIdPrefix(), "");
            String uri = "https://data.nbn.org.uk/api/taxa/" + nbnId + "/taxonomy";
            String response = getHttpClient().execute(new HttpGet(uri), new BasicResponseHandler());
            JsonNode taxa = new ObjectMapper().readTree(response);
            for (JsonNode jsonNode : taxa) {
                addTaxonNode(enriched, ids, names, ranks, jsonNode);
            }

            uri = "https://data.nbn.org.uk/api/taxa/" + nbnId;
            response = getHttpClient().execute(new HttpGet(uri), new BasicResponseHandler());
            JsonNode jsonNode = new ObjectMapper().readTree(response);
            addTaxonNode(enriched, ids, names, ranks, jsonNode);
        } catch (IOException e) {
            client = null;
            throw new PropertyEnricherException("failed to lookup [" + externalId + "]", e);
        }
        enriched.put(PropertyAndValueDictionary.PATH_IDS, toString(ids));
        enriched.put(PropertyAndValueDictionary.PATH, toString(names));
        enriched.put(PropertyAndValueDictionary.PATH_NAMES, toString(ranks));
    }

    protected void addTaxonNode(Map<String, String> enriched, List<String> ids, List<String> names, List<String> ranks, JsonNode jsonNode) {
        String externalId;
        externalId = jsonNode.has("taxonVersionKey") ? (TaxonomyProvider.NBN.getIdPrefix() + jsonNode.get("taxonVersionKey").asText()) : "";
        String name = jsonNode.has("name") ? jsonNode.get("name").asText() : "";
        String rank = jsonNode.has("rank") ? jsonNode.get("rank").asText() : "";
        enriched.put(PropertyAndValueDictionary.NAME, name);
        enriched.put(PropertyAndValueDictionary.RANK, rank);
        enriched.put(PropertyAndValueDictionary.EXTERNAL_ID, externalId);

        String commonName = jsonNode.has("commonName") ? jsonNode.get("commonName").asText() : "";
        if (StringUtils.isNotBlank(commonName)) {
            enriched.put(PropertyAndValueDictionary.COMMON_NAMES, commonName + " @en");
        }
        ids.add(externalId);
        names.add(name);
        ranks.add(rank);
    }

    protected String toString(List<String> ids) {
        return StringUtils.join(ids, CharsetConstant.SEPARATOR);
    }

    @Override
    public void shutdown() {

    }
}
