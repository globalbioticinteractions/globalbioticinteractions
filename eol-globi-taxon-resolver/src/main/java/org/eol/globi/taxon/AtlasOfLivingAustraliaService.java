package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasOfLivingAustraliaService implements PropertyEnricher {

    public static final String AFD_TSN_PREFIX = "urn:lsid:biodiversity.org.au:afd.taxon:";

    @Override
    public Map<String, String> enrich(final Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (StringUtils.isBlank(externalId) || hasSupportedExternalId(externalId)) {
            if (needsEnrichment(properties)) {
                String guid = StringUtils.replace(externalId, TaxonomyProvider.ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY, AFD_TSN_PREFIX);
                String taxonName = properties.get(PropertyAndValueDictionary.NAME);
                if (StringUtils.isBlank(guid) && StringUtils.length(taxonName) > 2) {
                    guid = findTaxonGUIDByName(taxonName);
                }
                if (StringUtils.isNotBlank(guid)) {
                    Map<String, String> taxonInfo = findTaxonInfoByGUID(guid);
                    enrichedProperties.putAll(taxonInfo);
                }
            }
        }
        return enrichedProperties;
    }

    private boolean hasSupportedExternalId(String externalId) throws PropertyEnricherException {
        return StringUtils.startsWith(externalId, TaxonomyProvider.ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY)
                || StringUtils.startsWith(externalId, AFD_TSN_PREFIX);
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

    private String findTaxonGUIDByName(String taxonName) throws PropertyEnricherException {
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
                        if (result.has("name") && result.has("idxtype") && result.has("guid")) {
                            if (StringUtils.equals(taxonName, result.get("name").getTextValue())
                                    && StringUtils.equals("TAXON", result.get("idxtype").getTextValue())) {
                                if (result.has("acceptedConceptID")) {
                                    guid = result.get("acceptedConceptID").asText();
                                } else {
                                    guid = result.get("guid").getTextValue();
                                }
                                break;
                            }
                        }
                    }
                }
            }


        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        } catch (JsonProcessingException e) {
            throw new PropertyEnricherException("failed to parse response", e);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to get response", e);
        }
        return guid;
    }

    protected Map<String, String> findTaxonInfoByGUID(String taxonGUID) throws PropertyEnricherException {
        Map<String, String> info = Collections.emptyMap();
        try {
            URI uri = taxonInfoByGUID(taxonGUID);
            String response = getResponse(uri);
            if (StringUtils.isNotBlank(response)) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(response);
                info = new HashMap<String, String>();
                if (node.has("taxonConcept")) {
                    info.putAll(parseTaxonConcept(node.get("taxonConcept")));
                }
                if (node.has("classification")) {
                    info.putAll(parseClassification(node.get("classification")));
                    final Taxon taxon = TaxonUtil.mapToTaxon(info);
                    taxon.setPath(taxon.getPath());
                    taxon.setPathIds(taxon.getPathIds());
                    taxon.setPathNames(taxon.getPathNames());
                    info.putAll(TaxonUtil.taxonToMap(taxon));
                }
                if (node.has("commonNames")) {
                    info.putAll(parseCommonName(node.get("commonNames")));
                }
            }
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        } catch (JsonProcessingException e) {
            throw new PropertyEnricherException("failed to parse response", e);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to get response", e);
        }
        return info;
    }

    private Map<String, String> parseCommonName(JsonNode commonNames) {
        final List<String> commonNameList = new ArrayList<String>();
        for (final JsonNode commonName : commonNames) {
            if (commonName.has("nameString") && commonName.has("language")) {
                commonNameList.add(commonName.get("nameString").getTextValue() + " @"  + commonName.get("language").getTextValue().split("-")[0]);
            }
        }
        return new HashMap<String, String>() {{
            if (commonNameList.size() > 0) {
                put(PropertyAndValueDictionary.COMMON_NAMES,
                        StringUtils.join(commonNameList, CharsetConstant.SEPARATOR));
            }
        }};
    }

    private Map<String, String> parseClassification(JsonNode classification) {
        Map<String, String> info = new HashMap<String, String>();

        String[] ranks = new String[]{
                "kingdom", "phylum", "subphylum", "class", "subclass", "order", "suborder", "superfamily", "family", "subfamily", "genus", "species"
        };
        List<String> path = new ArrayList<String>();
        List<String> pathIds = new ArrayList<String>();
        List<String> pathNames = new ArrayList<String>();

        for (String rank : ranks) {
            if (classification.has(rank)) {
                String textValue = classification.get(rank).getTextValue();
                path.add(StringUtils.capitalize(StringUtils.lowerCase(textValue)));
                pathNames.add(getRankString(rank));
                String guid = "";
                String guidName = rank + "Guid";
                if (classification.has(guidName)) {
                    guid = StringUtils.trim(classification.get(guidName).asText());
                }
                pathIds.add(guid);
            }
        }

        info.put(PropertyAndValueDictionary.PATH, StringUtils.join(path, CharsetConstant.SEPARATOR));
        info.put(PropertyAndValueDictionary.PATH_IDS, StringUtils.join(pathIds, CharsetConstant.SEPARATOR));
        info.put(PropertyAndValueDictionary.PATH_NAMES, StringUtils.join(pathNames, CharsetConstant.SEPARATOR));
        return info;
    }

    private Map<String, String> parseTaxonConcept(JsonNode classification) {
        Map<String, String> info = new HashMap<String, String>();
        if (classification.has("nameString")) {
            info.put(PropertyAndValueDictionary.NAME, classification.get("nameString").getTextValue());
        }

        if (classification.has("rankString")) {
            String rank = classification.get("rankString").getTextValue();
            info.put(PropertyAndValueDictionary.RANK, getRankString(rank));
        }

        if (classification.has("guid")) {
            String guid = classification.get("guid").getTextValue();
            String externalId = StringUtils.replace(guid, AFD_TSN_PREFIX, TaxonomyProvider.ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY);
            info.put(PropertyAndValueDictionary.EXTERNAL_ID, externalId);
        }

        return info;
    }

    private String getRankString(String rank) {
        return StringUtils.equals(rank, "clazz") ? "class" : rank;
    }

    private String getResponse(URI uri) throws PropertyEnricherException {
        HttpGet get = new HttpGet(uri);
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = HttpUtil.executeWithTimer(get, responseHandler);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup [" + uri.toString() + "]", e);
        }
        return response;
    }

    public void shutdown() {

    }
}
