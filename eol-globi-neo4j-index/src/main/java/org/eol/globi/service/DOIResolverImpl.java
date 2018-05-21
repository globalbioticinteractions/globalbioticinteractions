package org.eol.globi.service;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.HttpUtil;
import org.globalbioticinteractions.util.DOIUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DOIResolverImpl implements DOIResolver {
    private static final Log LOG = LogFactory.getLog(DOIResolverImpl.class);
    private final String baseURL;

    public DOIResolverImpl() {
        this("https://search.crossref.org/links");
    }

    public DOIResolverImpl(String baseURL) {
        this.baseURL = baseURL;
    }

    @Override
    public Map<String, String> resolveDoiFor(Collection<String> references) throws IOException {
        return requestLinks(references);
    }

    @Override
    public String resolveDoiFor(final String reference) throws IOException {
        ArrayList<String> references = new ArrayList<String>() {{
            add(reference);
        }};

        return requestLinks(references).getOrDefault(reference, null);
    }

    public Map<String, String> requestLinks(Collection<String> references) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        HttpPost post = new HttpPost(baseURL);
        post.setHeader("Content-Type", "application/json");
        StringEntity entity = new StringEntity(mapper.writeValueAsString(references), "UTF-8");
        post.setEntity(entity);

        BasicResponseHandler handler = new BasicResponseHandler();
        String response = HttpUtil.getHttpClient().execute(post, handler);
        JsonNode jsonNode = mapper.readTree(response);
        JsonNode results = jsonNode.get("results");
        Map<String, String> doiMap = new HashMap<>();
        if (jsonNode.get("query_ok").asBoolean()) {
            for (JsonNode result : results) {
                if (result.get("match").asBoolean()) {
                    String citation = result.get("text").getTextValue();
                    String doi = result.get("doi").getTextValue();
                    if (hasReasonableMatchScore(result) && StringUtils.isNoneBlank(citation, doi)) {
                        doiMap.put(citation, DOIUtil.URIfor(doi).toString());
                    }

                }
            }
        }
        return doiMap;
    }

    private boolean hasReasonableMatchScore(JsonNode result) {
        double score = 0.0;
        if (result.has("score")) {
            if (result.get("score").isDouble()) {
                score = result.get("score").getDoubleValue();
            }
        }
        return score > 50.0;
    }

    public String findCitationForDOI(String doi) throws IOException {
        String citation = null;
        try {
            URI uri = DOIUtil.URIfor(doi);
            if (uri != null) {
                citation = resolveCitation(uri);
            }
        } catch (IllegalArgumentException | ClientProtocolException ex) {
            LOG.warn("potentially malformed doi found [" + doi + "]", ex);
        }
        return citation;
    }

    public String resolveCitation(URI uri) throws IOException {
        String citation = null;
        HttpGet request = new HttpGet(uri);
        request.setHeader("Accept", "text/x-bibliography; style=council-of-science-editors; charset=UTF-8");
        request.setHeader("Accept-Charset", "UTF-8");
        HttpResponse response = HttpUtil.getHttpClient().execute(request);
        if (response.getStatusLine().getStatusCode() == 200) {
            citation = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
        } else {
            LOG.warn("failed to retrieve citation using [" + uri.toString() + "]: code [" + response.getStatusLine().getStatusCode() + "]:[" + response.getStatusLine().getReasonPhrase() + "]; content [" + IOUtils.toString(response.getEntity().getContent(), "UTF-8") + "]");
        }
        if (StringUtils.isNotBlank(citation)) {
            citation = citation.replaceFirst("^1\\. ", "").replace("\n", "");
        }
        return citation;
    }

}
