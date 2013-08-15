package org.eol.globi.service;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class EOLService extends BaseExternalIdService {

    @Override
    public boolean canLookupProperty(String propertyName) {
        return super.canLookupProperty(propertyName) || Taxon.PATH.equals(propertyName);
    }

    @Override
    public String lookupLSIDByTaxonName(String taxonName) throws TaxonPropertyLookupServiceException {
        Long pageId;

        try {
            pageId = getPageId(taxonName, true);
        } catch (URISyntaxException e) {
            throw new TaxonPropertyLookupServiceException("failed to create uri", e);
        }

        return pageId == null ? null : TaxonomyProvider.ID_PREFIX_EOL + pageId;
    }

    private URI createSearchURI(String taxonName) throws URISyntaxException {
        return new URI("http", null, "eol.org", 80, "/api/search/1.0/" + taxonName, "exact=true", null);
    }

    @Override
    public String lookupTaxonPathByLSID(String lsid) throws TaxonPropertyLookupServiceException {
        String path = null;
        if (lsid != null && lsid.startsWith(TaxonomyProvider.ID_PREFIX_EOL)) {
            Long pageId = Long.parseLong(lsid.replace(TaxonomyProvider.ID_PREFIX_EOL, ""));
            try {
                path = getRanks(pageId);
            } catch (URISyntaxException e) {
                throw new TaxonPropertyLookupServiceException("failed to create uri", e);
            } catch (JsonProcessingException e) {
                throw new TaxonPropertyLookupServiceException("failed to parse json", e);
            } catch (IOException e) {
                throw new TaxonPropertyLookupServiceException("failed to parse json", e);
            }
        }
        return path;
    }

    private String getRanks(Long pageId) throws URISyntaxException, TaxonPropertyLookupServiceException, IOException {
        StringBuilder ranks = new StringBuilder();
        URI uri = new URI("http", null, "eol.org", 80, "/api/pages/1.0/" + pageId, ".json?images=1&videos=0&sounds=0&maps=0&text=0&iucn=false&subjects=overview&licenses=all&details=false&common_names=true&synonyms=false&references=false&format=json", null);
        String response = getResponse(uri);
        if (response != null) {
            addRanks(ranks, response);
        }

        String s = ranks.toString();
        return s.isEmpty() ? null : s;
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
        uri = new URI("http", null, "eol.org", 80, "/api/hierarchy_entries/1.0/" + firstConceptId, ".json?common_names=false&synonyms=false&format=json", null);
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
                ranks.append(" ");
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

    private Long getPageId(String taxonName, boolean shouldFollowAlternate) throws TaxonPropertyLookupServiceException, URISyntaxException {
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
    }

    private String getResponse(URI uri) throws TaxonPropertyLookupServiceException {
        HttpGet get = new HttpGet(uri);

        BasicResponseHandler responseHandler = new BasicResponseHandler();

        HttpClient httpClient = getHttpClient();
        String response = null;
        try {
            response = httpClient.execute(get, responseHandler);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != 406 && e.getStatusCode() != 404) {
                throw new TaxonPropertyLookupServiceException("failed to lookup [" + uri.toString() + "]", e);
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
