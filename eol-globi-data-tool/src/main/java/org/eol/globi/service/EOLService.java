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
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Map;

public class EOLService extends BaseHttpClientService implements TaxonPropertyLookupService {

    @Override
    public void lookupPropertiesByName(String taxonName, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        Long id = null;
        if (properties.get(Taxon.EXTERNAL_ID) == null
                || properties.get(Taxon.PATH) == null
                || properties.get(Taxon.COMMON_NAMES) == null) {
            id = getPageId(taxonName, true);
            if (id != null && properties.get(Taxon.EXTERNAL_ID) == null) {
                properties.put(Taxon.EXTERNAL_ID, TaxonomyProvider.ID_PREFIX_EOL + id.toString());
            }
        }

        if (id != null) {
            if (properties.get(Taxon.PATH) == null || properties.get(Taxon.COMMON_NAMES) == null) {
                addPath(id, properties);
                String path = properties.get(Taxon.PATH);
                if (StringUtils.isNotBlank(path) && !path.contains(taxonName)) {
                    // add synonym
                    properties.put(Taxon.PATH, path + CharsetConstant.SEPARATOR + taxonName);
                }
            }
        }
    }

    private URI createSearchURI(String taxonName) throws URISyntaxException {
        String query = "q=" + taxonName.replaceAll("\\s", "+") + "&exact=true";
        return new URI("http", null, "eol.org", 80, "/api/search/1.0.xml", query, null);
    }

    protected void addPath(Long pageId, Map<String, String> properties) throws TaxonPropertyLookupServiceException {
        try {
            getRanks(pageId, properties);
        } catch (URISyntaxException e) {
            throw new TaxonPropertyLookupServiceException("failed to create uri", e);
        } catch (JsonProcessingException e) {
            throw new TaxonPropertyLookupServiceException("failed to parse response", e);
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("failed to get response", e);
        }
    }

    private void getRanks(Long pageId, Map<String, String> properties) throws URISyntaxException, TaxonPropertyLookupServiceException, IOException {
        URI uri = new URI("http", null, "eol.org", 80, "/api/pages/1.0/" + pageId + ".json", "images=1&videos=0&sounds=0&maps=0&text=0&iucn=false&subjects=overview&licenses=all&details=false&common_names=true&synonyms=false&references=false&format=json", null);
        String response = getResponse(uri);
        if (response != null) {
            if (properties.get(Taxon.PATH) == null) {
                StringBuilder ranks = new StringBuilder();
                addRanks(ranks, response);
                if (ranks.length() > 0) {
                    properties.put(Taxon.PATH, ranks.toString());
                }
            }

            if (properties.get(Taxon.COMMON_NAMES) == null) {
                StringBuilder commonNames = new StringBuilder();
                addCommonNames(commonNames, response);
                if (commonNames.length() > 0) {
                    properties.put(Taxon.COMMON_NAMES, commonNames.toString());
                }
            }
        }
    }

    private void addCommonNames(StringBuilder commonNames, String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response);

        JsonNode vernacularNames = node.get("vernacularNames");
        for (JsonNode vernacularName : vernacularNames) {
            if (vernacularName.has("eol_preferred")) {
                String languageCode = vernacularName.get("language").getValueAsText();
                String commonName = vernacularName.get("vernacularName").getValueAsText();
                if (StringUtils.isNotBlank(languageCode) && StringUtils.isNotBlank(commonName)) {
                    commonNames.append(commonName);
                    commonNames.append(" @");
                    commonNames.append(languageCode);
                    commonNames.append(CharsetConstant.SEPARATOR);
                }

            }
        }


    }

    private void addRanks(StringBuilder ranks, String response) throws IOException, URISyntaxException, TaxonPropertyLookupServiceException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response);

        JsonNode taxonConcepts = node.get("taxonConcepts");
        String firstConceptId = null;
        for (JsonNode taxonConcept : taxonConcepts) {
            if (taxonConcept.has("identifier")) {
                firstConceptId = taxonConcept.get("identifier").getValueAsText();
                break;
            }
            ;
        }
        if (firstConceptId != null) {
            addRanks(firstConceptId, ranks);
        }
    }

    private void addRanks(String firstConceptId, StringBuilder ranks) throws URISyntaxException, TaxonPropertyLookupServiceException, IOException {
        URI uri;
        String response;
        uri = new URI("http", null, "eol.org", 80, "/api/hierarchy_entries/1.0/" + firstConceptId + ".json", "common_names=false&synonyms=false&format=json", null);
        response = getResponse(uri);
        if (response != null) {
            parseResponse(ranks, response);
        }
    }

    protected void parseResponse(StringBuilder ranks, String response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response);
        JsonNode ancestors = node.get("ancestors");

        boolean isFirst = true;
        for (JsonNode ancestor : ancestors) {
            isFirst = parseTaxonNode(ranks, isFirst, ancestor);
        }

        parseTaxonNode(ranks, isFirst, node);
    }

    private boolean parseTaxonNode(StringBuilder ranks, boolean first, JsonNode ancestor) {
        if (ancestor.has("scientificName")) {
            if (!first) {
                ranks.append(CharsetConstant.SEPARATOR);
            }
            String scientificName = ancestor.get("scientificName").getTextValue();
            String[] split = scientificName.split(" ");
            ranks.append(split[0]);
            if (split.length > 1 && ancestor.has("taxonRank") && "Species".equals(ancestor.get("taxonRank").getTextValue())) {
                ranks.append(" ");
                ranks.append(split[1]);
            }

            first = false;
        }
        return first;
    }

    protected Long getPageId(String taxonName, boolean shouldFollowAlternate) throws TaxonPropertyLookupServiceException {
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
                                throw new TaxonPropertyLookupServiceException("failed to decode [" + alternateTaxonName + "]", e);
                            }

                        }

                    }

                }
            }
            return smallestPageId;
        } catch (URISyntaxException e) {
            throw new TaxonPropertyLookupServiceException("failed to fetch pageid for [" + taxonName + "]", e);
        }

    }

    private String getResponse(URI uri) throws TaxonPropertyLookupServiceException {
        HttpGet get = new HttpGet(uri);
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        String response = null;
        try {
            response = execute(get, responseHandler);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != 406 && e.getStatusCode() != 404) {
                throw new TaxonPropertyLookupServiceException("failed to lookup [" + uri.toString() + "]: http status [" + e.getStatusCode() + "]   ", e);
            }
        } catch (ClientProtocolException e) {
            throw new TaxonPropertyLookupServiceException("failed to lookup [" + uri.toString() + "]", e);
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("failed to lookup [" + uri.toString() + "]", e);
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
}
