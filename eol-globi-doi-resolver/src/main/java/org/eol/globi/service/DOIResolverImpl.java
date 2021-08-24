package org.eol.globi.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Map;

public class DOIResolverImpl implements DOIResolver {
    private static final Logger LOG = LoggerFactory.getLogger(DOIResolverImpl.class);
    private final String baseURL;

    private double minMatchScore = 100.0;

    public DOIResolverImpl() {
        this("https://api.crossref.org");
    }

    public DOIResolverImpl(String baseURL) {
        this.baseURL = baseURL;
    }

    @Override
    public Map<String, DOI> resolveDoiFor(Collection<String> references) throws IOException {
        return requestLinks(references);
    }

    @Override
    public DOI resolveDoiFor(final String reference) throws IOException {
        ArrayList<String> references = new ArrayList<String>() {{
            add(reference);
        }};

        return requestLinks(references).getOrDefault(reference, null);
    }

    private Map<String, DOI> requestLinks(Collection<String> references) throws IOException {
        Map<String, DOI> doiMap = new TreeMap<>();
        for (String reference : references) {
            try {
                URIBuilder builder = new URIBuilder(baseURL + "/works");
                builder.addParameter("sort", "score");
                builder.addParameter("order", "desc");
                builder.addParameter("rows", "1");
                builder.addParameter("select", "DOI,score");
                builder.addParameter("query.bibliographic", reference);
                HttpGet get = new HttpGet(builder.build());
                get.setHeader("Content-Type", "application/json");
                doiMap.put(reference, getMostRelevantDOIMatch(get));
            } catch (URISyntaxException e) {
                LOG.warn("unexpected malformed URI on resolving crossref dois", e);
            } catch (MalformedDOIException e) {
                LOG.warn("received malformed doi from cross ref", e);
            }
        }
        return doiMap;
    }

    private DOI getMostRelevantDOIMatch(HttpGet get) throws IOException, MalformedDOIException {
        ResponseHandler<String> handler = HttpUtil.createUTF8BasicResponseHandler();
        String response = HttpUtil.getHttpClient().execute(get, handler);
        return extractDOI(response);
    }

    DOI extractDOI(String response) throws IOException, MalformedDOIException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response);
        DOI doi = null;
        if (jsonNode.has("message")) {
            JsonNode msg = jsonNode.get("message");
                if (msg.has("items")) {
                    for (JsonNode items : msg.get("items")) {
                        if (hasReasonableMatchScore(items)) {
                            doi = DOI.create(items.get("DOI").asText());
                        }
                    }
                }
        }
        return doi;
    }

    private boolean hasReasonableMatchScore(JsonNode result) {
        double score = 0.0;
        if (result.has("score")) {
            if (result.get("score").isDouble()) {
                score = result.get("score").asDouble();
            }
        }
        return score > minMatchScore;
    }

    public String findCitationForDOI(DOI doi) throws IOException {
        String citation = null;
        if (doi != null) {
            HttpGet request = new HttpGet(doi.toURI());
            request.setHeader("Accept", "text/x-bibliography; style=council-of-science-editors; charset=UTF-8");
            request.setHeader("Accept-Charset", "UTF-8");
            HttpResponse response = HttpUtil.getHttpClient().execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                citation = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            } else {
                LOG.warn("failed to retrieve citation using [" + doi.toString() + "]: code [" + response.getStatusLine().getStatusCode() + "]:[" + response.getStatusLine().getReasonPhrase() + "]; content [" + IOUtils.toString(response.getEntity().getContent(), "UTF-8") + "]");
            }
            if (StringUtils.isNotBlank(citation)) {
                citation = citation.replaceFirst("^1\\. ", "").replace("\n", "");
            }
        }
        return citation;
    }

    public void setMinMatchScore(double minMatchScore) {
        this.minMatchScore = minMatchScore;
    }



}
