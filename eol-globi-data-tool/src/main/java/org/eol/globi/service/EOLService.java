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
import java.net.URI;
import java.net.URISyntaxException;

public class EOLService extends BaseExternalIdService {

    @Override
    public boolean canLookupProperty(String propertyName) {
        return super.canLookupProperty(propertyName) || Taxon.PATH.equals(propertyName);
    }

    @Override
    public String lookupLSIDByTaxonName(String taxonName) throws TaxonPropertyLookupServiceException {
        String pageId;

        try {
            URI uri = new URI("http", null, "eol.org", 80, "/api/search/1.0/" + taxonName, "exact=true", null);
            pageId = getPageId(taxonName, uri, true);
        } catch (URISyntaxException e) {
            throw new TaxonPropertyLookupServiceException("failed to create uri", e);
        }

        return pageId == null ? null : TaxonomyProvider.ID_PREFIX_EOL + pageId;
    }

    @Override
    public String lookupTaxonPathByLSID(String lsid) throws TaxonPropertyLookupServiceException {
        String path = null;
        if (lsid != null && lsid.startsWith(TaxonomyProvider.ID_PREFIX_EOL)) {
            Long pageId = Long.parseLong(lsid.replace(TaxonomyProvider.ID_PREFIX_EOL, ""));
            try {
                path = getRanks(lsid, pageId);
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

    private String getRanks(String lsid, Long pageId) throws URISyntaxException, TaxonPropertyLookupServiceException, IOException {
        StringBuilder ranks = new StringBuilder();
        URI uri = new URI("http", null, "eol.org", 80, "/api/pages/1.0/" + pageId, ".json?images=1&videos=0&sounds=0&maps=0&text=0&iucn=false&subjects=overview&licenses=all&details=false&common_names=true&synonyms=false&references=false&format=json", null);
        String response = getResponse(lsid, uri);
        if (response != null) {
            addRanks(lsid, ranks, response);
        }

        String s = ranks.toString();
        return s.isEmpty() ? null : s;
    }

    private void addRanks(String lsid, StringBuilder ranks, String response) throws IOException, URISyntaxException, TaxonPropertyLookupServiceException {
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
        addRanks(lsid, firstConceptId, ranks);
    }

    private void addRanks(String lsid, String firstConceptId, StringBuilder ranks) throws URISyntaxException, TaxonPropertyLookupServiceException, IOException {
        URI uri;
        String response;
        ObjectMapper mapper;
        JsonNode node;
        uri = new URI("http", null, "eol.org", 80, "/api/hierarchy_entries/1.0/" + firstConceptId, ".json?common_names=false&synonyms=false&format=json", null);
        response = getResponse(lsid, uri);
        if (response != null) {
            mapper = new ObjectMapper();
            node = mapper.readTree(response);
            JsonNode ancestors = node.get("ancestors");

            boolean isFirst = true;
            for (JsonNode ancestor : ancestors) {
                if (ancestor.has("scientificName")) {
                    if (!isFirst) {
                        ranks.append(" ");
                    }
                    String scientificName = ancestor.get("scientificName").getTextValue();
                    ranks.append(scientificName.split(" ")[0]);
                    isFirst = false;
                }
            }
        }
    }

    private String getPageId(String taxonName, URI uri, boolean shouldFollowAlternate) throws TaxonPropertyLookupServiceException, URISyntaxException {
        String response = getResponse(taxonName, uri);


        String pageId = null;

        if (response != null) {
            // pick first of non empty result, assuming that exact match parameter is yielding a valid result
            if (!response.contains("totalResults>0<")) {
                String[] strings = response.split("<entry>");
                if (strings.length > 1) {
                    String[] anotherSplit = strings[1].split("<id>");
                    if (anotherSplit.length > 1) {
                        String[] yetAnotherSplit = anotherSplit[1].split("</id>");
                        pageId = yetAnotherSplit.length > 1 ? yetAnotherSplit[0].trim() : null;
                    }
                }
            } else if (shouldFollowAlternate) {
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

    private String getResponse(String queryValue, URI uri) throws TaxonPropertyLookupServiceException {
        HttpGet get = new HttpGet(uri);

        BasicResponseHandler responseHandler = new BasicResponseHandler();

        HttpClient httpClient = getHttpClient();
        String response = null;
        try {
            response = httpClient.execute(get, responseHandler);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() != 406 && e.getStatusCode() != 404) {
                throw new TaxonPropertyLookupServiceException("failed to lookup [" + queryValue + "]", e);
            }
        } catch (ClientProtocolException e) {
            throw new TaxonPropertyLookupServiceException("failed to lookup [" + queryValue + "]", e);
        } catch (IOException e) {
            throw new TaxonPropertyLookupServiceException("failed to lookup [" + queryValue + "]", e);
        }
        return response;
    }

}
