package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EOLService implements PropertyEnricher {

    private PropertyEnrichmentFilter filter = new PropertyEnrichmentFilterExternalId();

    @Override
    public Map<String, String> enrich(final Map<String, String> properties) throws PropertyEnricherException {
        Map<String, String> enrichedProperties = new HashMap<String, String>(properties);
        String externalId = enrichedProperties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (needsEnrichment(enrichedProperties)) {
            Long id = getEOLPageId(enrichedProperties.get(PropertyAndValueDictionary.NAME), externalId);
            if (id != null) {
                addExternalId(enrichedProperties, id);
                addTaxonInfo(id, enrichedProperties);
                if (filter.shouldReject(enrichedProperties)) {
                    enrichedProperties.put(PropertyAndValueDictionary.NAME, null);
                    enrichedProperties.put(PropertyAndValueDictionary.COMMON_NAMES, null);
                    enrichedProperties.put(PropertyAndValueDictionary.EXTERNAL_ID, null);
                    enrichedProperties.put(PropertyAndValueDictionary.PATH, null);
                    enrichedProperties.put(PropertyAndValueDictionary.PATH_NAMES, null);
                }
            }
        }
        return enrichedProperties;
    }

    private Long getEOLPageId(String name, String externalId) throws PropertyEnricherException {
        Long id = null;
        if (ExternalIdUtil.isSupported(externalId)) {
            if (externalId.startsWith(TaxonomyProvider.ID_PREFIX_EOL)) {
                String eolPageIdString = externalId.replaceFirst(TaxonomyProvider.ID_PREFIX_EOL, "");
                try {
                    id = Long.parseLong(eolPageIdString);
                } catch (NumberFormatException ex) {
                    throw new PropertyEnricherException("failed to parse eol id [" + eolPageIdString + "]");
                }
            } else if (externalId.startsWith(TaxonomyProvider.NCBI.getIdPrefix())) {
                id = getPageIdFromProvider(1172, externalId.replace(TaxonomyProvider.NCBI.getIdPrefix(), ""));
            } else if (externalId.startsWith(TaxonomyProvider.ITIS.getIdPrefix())) {
                id = getPageIdFromProvider(903, externalId.replace(TaxonomyProvider.ITIS.getIdPrefix(), ""));
            }
        } else if (StringUtils.isNotBlank(name) && !PropertyAndValueDictionary.NO_NAME.equals(name)) {
            id = getPageId(name, true);
        }
        return id;
    }

    private Long getPageIdFromProvider(int eolProviderId, String providerTaxonId) throws PropertyEnricherException {
        Long eolPageId = null;
        try {
            URI uri1 = new URI("http://eol.org/api/search_by_provider/1.0/" + providerTaxonId + ".json?hierarchy_id=" + eolProviderId);
            String response1 = getResponse(uri1);
            if (response1 == null) {
                throw new PropertyEnricherException("failed to retrieve response for [" + uri1 + "]");
            }
            JsonNode jsonNode = new ObjectMapper().readTree(response1);
            if (jsonNode.isArray() && jsonNode.size() > 0) {
                JsonNode jsonNode1 = jsonNode.get(0);
                if (jsonNode1.has("eol_page_id")) {
                    eolPageId = Long.parseLong(jsonNode1.get("eol_page_id").asText());
                }
            }
        } catch (JsonProcessingException ex) {
            throw new PropertyEnricherException("failed to create uri", ex);
        } catch (URISyntaxException ex) {
            throw new PropertyEnricherException("failed to create uri", ex);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to get response", e);
        } catch (NumberFormatException e) {
            throw new PropertyEnricherException("invalid page id", e);
        }
        return eolPageId;
    }

    private boolean needsEnrichment(Map<String, String> properties) {
        return StringUtils.isBlank(properties.get(PropertyAndValueDictionary.PATH))
                || StringUtils.isBlank(properties.get(PropertyAndValueDictionary.COMMON_NAMES));
    }

    private URI createSearchURI(String taxonName) throws URISyntaxException {
        String query = "q=" + taxonName.replaceAll("\\s", "+") + "&exact=true";
        return new URI("http", null, "eol.org", 80, "/api/search/1.0.xml", query, null);
    }

    protected void addTaxonInfo(Long pageId, Map<String, String> properties) throws PropertyEnricherException {
        try {
            URI uri = new URI("http", null, "eol.org", 80, "/api/pages/1.0/" + pageId + ".json", "images=0&videos=0&sounds=0&maps=0&text=0&iucn=false&subjects=overview&licenses=all&details=false&common_names=true&synonyms=false&references=false&format=json", null);
            String response = getResponse(uri);
            if (response != null) {
                addCanonicalNamesAndRanks(properties, response);

                StringBuilder commonNames = new StringBuilder();
                addCommonNames(commonNames, response);

                if (commonNames.length() > 0) {
                    properties.put(PropertyAndValueDictionary.COMMON_NAMES, commonNames.toString());
                }
            }
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to create uri", e);
        } catch (JsonProcessingException e) {
            throw new PropertyEnricherException("failed to parse response", e);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to get response", e);
        }
    }

    private void addCanonicalNamesAndRanks(Map<String, String> properties, String response) throws IOException, URISyntaxException, PropertyEnricherException {
        List<String> ranks = new ArrayList<String>();
        List<String> rankNames = new ArrayList<String>();
        List<String> rankIds = new ArrayList<String>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response);
        if (node.has("identifier")) {
            Long resolvedPageId = node.get("identifier").asLong();
            addExternalId(properties, resolvedPageId);
        }
        JsonNode taxonConcepts = node.get("taxonConcepts");
        String firstConceptId = null;
        for (JsonNode taxonConcept : taxonConcepts) {
            if (taxonConcept.has("identifier")) {
                firstConceptId = taxonConcept.get("identifier").asText();
                String accordingTo = nameAccordingTo(taxonConcept);
                if (!StringUtils.contains(accordingTo, "IUCN")) {
                    if (accordingToNCBI(accordingTo) && taxonConcept.has("scientificName")) {
                        properties.put(PropertyAndValueDictionary.NAME, taxonConcept.get("scientificName").asText());
                    } else if (taxonConcept.has("canonicalForm")) {
                        properties.put(PropertyAndValueDictionary.NAME, taxonConcept.get("canonicalForm").asText());
                    }
                    String taxonRank = rankOf(taxonConcept);
                    if (StringUtils.isEmpty(taxonRank)) {
                        String name = properties.get(PropertyAndValueDictionary.NAME);
                        if (isProbablyFishBaseSpecies(accordingTo, name)) {
                            properties.put(PropertyAndValueDictionary.RANK, "Species");
                        }
                    } else {
                        properties.put(PropertyAndValueDictionary.RANK, taxonConcept.get("taxonRank").asText());
                    }
                    break;
                }
            }
        }
        if (firstConceptId != null) {
            addRanks(firstConceptId, ranks, rankNames, rankIds);
        }

        if (ranks.size() > 0) {
            properties.put(PropertyAndValueDictionary.PATH, StringUtils.join(ranks, CharsetConstant.SEPARATOR));
            if (rankNames.size() == ranks.size()) {
                properties.put(PropertyAndValueDictionary.PATH_NAMES, StringUtils.join(rankNames, CharsetConstant.SEPARATOR));
            }
            if (rankIds.size() == ranks.size()) {
                properties.put(PropertyAndValueDictionary.PATH_IDS, StringUtils.join(rankIds, CharsetConstant.SEPARATOR));
            }
        }


    }

    // workaround related to https://github.com/jhpoelen/gomexsi/issues/92 - fishbase species names do not have taxonRank
    private boolean isProbablyFishBaseSpecies(String accordingTo, String name) {
        return StringUtils.contains(accordingTo, "FishBase") && StringUtils.split(name).length > 1;
    }

    private boolean accordingToNCBI(String accordingTo) {
        return StringUtils.contains(accordingTo, "NCBI");
    }

    private String nameAccordingTo(JsonNode taxonConcept) {
        return taxonConcept.has("nameAccordingTo") ? getNameAccordingTo(taxonConcept) : null;
    }

    private String getNameAccordingTo(JsonNode taxonConcept) {
        String accordingTo;
        JsonNode node = taxonConcept.get("nameAccordingTo");
        if (node.isArray() && node.size() > 0) {
            accordingTo = node.get(0).asText();
        } else {
            accordingTo = node.asText();
        }
        return accordingTo;
    }

    private void addExternalId(Map<String, String> properties, Long resolvedPageId) {
        properties.put(PropertyAndValueDictionary.EXTERNAL_ID, TaxonomyProvider.ID_PREFIX_EOL + resolvedPageId);
    }

    private void addCommonNames(StringBuilder commonNames, String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response);

        JsonNode vernacularNames = node.get("vernacularNames");
        for (JsonNode vernacularName : vernacularNames) {
            if (vernacularName.has("eol_preferred")) {
                String languageCode = vernacularName.get("language").asText();
                String commonName = vernacularName.get("vernacularName").asText();
                if (StringUtils.isNotBlank(languageCode) && StringUtils.isNotBlank(commonName)) {
                    commonNames.append(commonName);
                    commonNames.append(" @");
                    commonNames.append(languageCode);
                    commonNames.append(CharsetConstant.SEPARATOR);
                }

            }
        }


    }

    private void addRanks(String firstConceptId, List<String> ranks, List<String> rankNames, List<String> rankIds) throws URISyntaxException, PropertyEnricherException, IOException {
        URI uri;
        String response;
        uri = new URI("http", null, "eol.org", 80, "/api/hierarchy_entries/1.0/" + firstConceptId + ".json", "common_names=false&synonyms=false&format=json", null);
        response = getResponse(uri);
        if (response != null) {
            parseRankResponse(response, ranks, rankNames, rankIds);
        }
    }

    protected void parseRankResponse(String response, List<String> ranks, List<String> rankNames, List<String> rankIds) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response);
        String accordingTo = nameAccordingTo(node);

        JsonNode ancestors = node.get("ancestors");
        for (JsonNode ancestor : ancestors) {
            parseTaxonNode(ancestor, ranks, rankNames, rankIds, accordingTo);
        }

        parseTaxonNode(node, ranks, rankNames, rankIds, accordingTo);
    }

    private void parseTaxonNode(JsonNode ancestor, List<String> ranks, List<String> rankNames, List<String> rankIds, String accordingTo) {
        String scientificName = ancestor.has("scientificName") ? ancestor.get("scientificName").getTextValue() : null;
        scientificName = StringUtils.containsIgnoreCase(scientificName, "Not Assigned") ? "" : scientificName;
        if (null != scientificName) {
            String taxonRank = StringUtils.isBlank(scientificName) ? "" : rankOf(ancestor);
            if (isProbablyFishBaseSpecies(accordingTo, scientificName)) {
                taxonRank = "species";
            }
            rankNames.add(taxonRank);

            if (accordingToNCBI(accordingTo)) {
                ranks.add(scientificName);
            } else {
                String[] split = scientificName.split(" ");
                String name = split[0];
                if (split.length > 0) {
                    if (StringUtils.contains(taxonRank, "species")) {
                        if (split.length > 1 && StringUtils.equals(taxonRank, "species")) {
                            name += " " + split[1];
                        } else if (split.length > 1) {
                            if (split.length > 1) {
                                name += " " + split[1];
                            }
                            if (split.length > 2) {
                                name += " " + split[2];
                            }
                        }
                    }
                }
                ranks.add(name);
            }

            String taxonConceptId = "";
            if (StringUtils.isNotBlank(scientificName) && ancestor.has("taxonConceptID")) {
                taxonConceptId = TaxonomyProvider.ID_PREFIX_EOL + ancestor.get("taxonConceptID").asText();
            }
            rankIds.add(taxonConceptId);
        }


    }


    private String rankOf(JsonNode ancestor) {
        String taxonRank = "";
        if (ancestor.has("taxonRank")) {
            taxonRank = ancestor.get("taxonRank").getTextValue().toLowerCase();
        }
        return taxonRank;
    }

    protected Long getPageId(String taxonName, boolean shouldFollowAlternate) throws PropertyEnricherException {
        try {
            URI uri = createSearchURI(taxonName);
            String response = getResponse(uri);
            Long smallestPageId = null;

            if (response != null) {
                // pick first of non empty result, assuming that exact match parameter is yielding a valid result
                if (!response.contains("totalResults>0<")) {
                    smallestPageId = findSmallestPageId(response);

                } else if (shouldFollowAlternate) {
                    String[] alternates = response.split("<link rel=\"alternate\" href=\"http://eol.org/api/search/1.0/");
                    if (alternates.length > 1) {
                        String[] urlSplit = alternates[1].split("\"");
                        if (urlSplit.length > 1) {
                            String alternateTaxonName = urlSplit[0];
                            try {
                                String decodedName = URLDecoder.decode(alternateTaxonName, "UTF-8");
                                decodedName = decodedName.replace("/", "");
                                if (!decodedName.equals(taxonName)) {
                                    smallestPageId = getPageId(decodedName, false);
                                }
                            } catch (UnsupportedEncodingException e) {
                                throw new PropertyEnricherException("failed to decode [" + alternateTaxonName + "]", e);
                            }

                        }

                    }

                }
            }
            return smallestPageId;
        } catch (URISyntaxException e) {
            throw new PropertyEnricherException("failed to fetch pageid for [" + taxonName + "]", e);
        }

    }

    private String getResponse(URI uri) throws PropertyEnricherException {
        HttpGet get = new HttpGet(uri);
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response = null;
        try {
            response = HttpUtil.executeWithTimer(get, responseHandler);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != 406 && e.getStatusCode() != 404) {
                throw new PropertyEnricherException("failed to lookup [" + uri.toString() + "]: http status [" + e.getStatusCode() + "]   ", e);
            }
        } catch (ClientProtocolException e) {
            throw new PropertyEnricherException("failed to lookup [" + uri.toString() + "]", e);
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup [" + uri.toString() + "]", e);
        }
        return response;
    }

    protected Long findSmallestPageId(String response) {
        Long smallestPageId = null;
        String[] entries = response.split("<entry>");
        for (int i = 1; i < entries.length; i++) {
            String[] anotherSplit = entries[i].split("<id>");
            if (anotherSplit.length > 1) {
                String[] yetAnotherSplit = anotherSplit[1].split("</id>");
                String pageId = yetAnotherSplit.length > 1 ? yetAnotherSplit[0].trim() : null;
                if (pageId != null) {
                    long pageIdNumber = Long.parseLong(pageId);
                    smallestPageId = (smallestPageId == null || smallestPageId > pageIdNumber) ? pageIdNumber : smallestPageId;
                }
            }
        }
        return smallestPageId;
    }

    public void setFilter(PropertyEnrichmentFilter filter) {
        this.filter = filter;
    }

    public void shutdown() {

    }
}
