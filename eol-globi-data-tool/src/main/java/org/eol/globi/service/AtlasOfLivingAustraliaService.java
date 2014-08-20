package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasOfLivingAustraliaService extends BaseHttpClientService implements TaxonPropertyLookupService {

    @Override
    public void lookupPropertiesByName(String name, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (needsEnrichment(properties)) {
            String guid = hasValidGUID(externalId) ? externalId : findTaxonGUIDByName(name);

            if (hasValidGUID(guid)) {
                Map<String, String> taxonInfo = findTaxonInfoByGUID(guid);
                properties.putAll(taxonInfo);
            }
        }
    }

    private boolean hasValidGUID(String externalId) throws TaxonPropertyLookupServiceException {
        return StringUtils.startsWith(externalId, TaxonomyProvider.ID_PREFIX_LIVING_ATLAS_OF_AUSTRALIA);
    }

    private boolean needsEnrichment(Map<String, String> properties) {
        return StringUtils.isBlank(properties.get(PropertyAndValueDictionary.PATH))
                || StringUtils.isBlank(properties.get(PropertyAndValueDictionary.COMMON_NAMES));
    }

    private URI taxonInfoByGUID(String taxonGUID) throws URISyntaxException {
        return new URI("http", null, "bie.ala.org.au", 80, "/ws/species/" + taxonGUID + ".json", null, null);
    }

    private URI taxonInfoByName(String taxonName) throws URISyntaxException {
        return new URI("http", null, "bie.ala.org.au", 80, "/ws/search.json", "q=" + taxonName, null);
    }

    private String findTaxonGUIDByName(String taxonName) throws TaxonPropertyLookupServiceException {
        String guid = null;
        try {
            URI uri = taxonInfoByName(taxonName);
            String response = getResponse(uri);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            if (node.has("searchResults")) {
                JsonNode searchResults = node.get("searchResults");
                if (searchResults.has("results")) {
                    JsonNode results = searchResults.get("results");
                    for (JsonNode result : results) {
                        if (result.has("name") && result.has("idxType") && result.has("guid")) {
                            if (StringUtils.equals(taxonName, result.get("name").getTextValue())
                                    && StringUtils.equals("TAXON", result.get("idxType").getTextValue())) {
                                guid = result.get("guid").getTextValue();
                                break;
                            }
                        }
                    }
                }
            }


        } catch (URISyntaxException e) {
            throw new TaxonPropertyLookupServiceException("failed to create uri", e);
        } catch (JsonProcessingException e) {
            throw new TaxonPropertyLookupServiceException("failed to parse response", e);
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("failed to get response", e);
        }
        return guid;
    }

    protected Map<String, String> findTaxonInfoByGUID(String taxonGUID) throws TaxonPropertyLookupServiceException {
        Map<String, String> info = Collections.emptyMap();
        try {
            URI uri = taxonInfoByGUID(taxonGUID);
            String response = getResponse(uri);
            if (StringUtils.isNotBlank(response)) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(response);
                info = new HashMap<String, String>();
                if (node.has("classification")) {
                    info.putAll(parseClassification(node.get("classification")));
                }
                if (node.has("commonNames")) {
                    info.putAll(parseCommonName(node.get("commonNames")));
                }


            }
        } catch (URISyntaxException e) {
            throw new TaxonPropertyLookupServiceException("failed to create uri", e);
        } catch (JsonProcessingException e) {
            throw new TaxonPropertyLookupServiceException("failed to parse response", e);
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("failed to get response", e);
        }
        return info;
    }

    private Map<String, String> parseCommonName(JsonNode commonNames) {
        Map<String, String> info = Collections.emptyMap();
        for (final JsonNode commonName : commonNames) {
            if (commonName.has("nameString")
                    && commonName.has("isPreferred")
                    && commonName.get("isPreferred").getBooleanValue()) {
                info = new HashMap<String, String>() {{
                    put(PropertyAndValueDictionary.COMMON_NAMES,
                            commonName.get("nameString").getTextValue() + " @en");
                }};
            }
        }
        return info;
    }

    private Map<String, String> parseClassification(JsonNode classification) {
        Map<String, String> info = new HashMap<String, String>();
        if (classification.has("scientificName")) {
            info.put(PropertyAndValueDictionary.NAME, classification.get("scientificName").getTextValue());
        }

        String[] ranks = new String[]{
                "kingdom", "phylum", "clazz", "order", "family", "genus", "species"
        };
        List<String> path = new ArrayList<String>();
        List<String> pathNames = new ArrayList<String>();
        if (classification.has("rank")) {
            String rank = classification.get("rank").getTextValue();
            info.put(PropertyAndValueDictionary.RANK, getRankString(rank));
        }

        if (classification.has("guid")) {
            String guid = classification.get("guid").getTextValue();
            info.put(PropertyAndValueDictionary.EXTERNAL_ID, guid);
        }

        for (String rank : ranks) {
            if (classification.has(rank)) {
                String textValue = classification.get(rank).getTextValue();
                path.add(StringUtils.capitalize(StringUtils.lowerCase(textValue)));
                pathNames.add(getRankString(rank));
            }
        }
        info.put(PropertyAndValueDictionary.PATH, StringUtils.join(path, CharsetConstant.SEPARATOR));
        info.put(PropertyAndValueDictionary.PATH_NAMES, StringUtils.join(pathNames, CharsetConstant.SEPARATOR));
        return info;
    }

    private String getRankString(String rank) {
        return StringUtils.equals(rank, "clazz") ? "class" : rank;
    }

    private String getResponse(URI uri) throws TaxonPropertyLookupServiceException {
        HttpGet get = new HttpGet(uri);
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = execute(get, responseHandler);
        } catch (ClientProtocolException e) {
            throw new TaxonPropertyLookupServiceException("failed to lookup [" + uri.toString() + "]", e);
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("failed to lookup [" + uri.toString() + "]", e);
        }
        return response;
    }
}
