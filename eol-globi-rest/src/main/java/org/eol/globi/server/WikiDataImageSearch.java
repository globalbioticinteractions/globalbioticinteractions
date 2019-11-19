package org.eol.globi.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.ImageSearch;
import org.eol.globi.service.SearchContext;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class WikiDataImageSearch implements ImageSearch {

    private static Log LOG = LogFactory.getLog(WikiDataImageSearch.class);

    @Override
    public TaxonImage lookupImageForExternalId(String externalId) throws IOException {
        return StringUtils.startsWithIgnoreCase(externalId, TaxonomyProvider.WIKIDATA.getIdPrefix())
                ? requestImage(externalId, () -> "en")
                : null;
    }

    @Override
    public TaxonImage lookupImageForExternalId(String externalId, SearchContext context) throws IOException {
        return StringUtils.startsWithIgnoreCase(externalId, TaxonomyProvider.WIKIDATA.getIdPrefix())
                ? requestImage(externalId, context)
                : null;
    }

    private TaxonImage requestImage(String externalId, SearchContext context) throws IOException {
        TaxonImage taxonImage = null;
        String wikiDataId = StringUtils.replace(externalId, TaxonomyProvider.WIKIDATA.getIdPrefix(), "");

        String languageCode = "en";
        String sparql = "SELECT ?item ?pic ?name WHERE { \n" +
                "  wd:" + wikiDataId + " wdt:P18 ?pic . \n" +
                "  SERVICE wikibase:label {\n" +
                "    bd:serviceParam wikibase:language \"" + context.getPreferredLanguage() + "\" .\n" +
                "    wd:Q140 wdt:P1843 ?name .\n" +
                "  }\n" +
                "} limit 1";

        try {
            URI url = new URI("https", "query.wikidata.org", "/sparql", "query=" + sparql, null);
            LOG.info("requesting image [" + url + "]");
            HttpGet httpGet = HttpUtil.httpGetJson(url);
            String jsonString = HttpUtil.executeAndRelease(httpGet, HttpUtil.getFailFastHttpClient());
            return parseWikidataResult(externalId, taxonImage, jsonString, context);

        } catch (URISyntaxException e) {
            throw new IOException("marlformed uri", e);
        }

    }

    private TaxonImage parseWikidataResult(String externalId, TaxonImage taxonImage, String jsonString, SearchContext context) throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
        if (jsonNode.has("results")) {
            JsonNode results = jsonNode.get("results");
            if (results.has("bindings")) {
                JsonNode bindings = results.get("bindings");
                for (JsonNode binding : bindings) {
                    taxonImage = new TaxonImage();
                    taxonImage.setInfoURL(ExternalIdUtil.urlForExternalId(externalId));
                    JsonNode pic = binding.get("pic");
                    if (valueExists(pic)) {
                        taxonImage.setThumbnailURL(StringUtils.replace(pic.get("value").asText(), "http:", "https:") + "?width=100");
                    }
                    JsonNode name = binding.get("name");
                    if (valueExists(name)) {
                        taxonImage.setCommonName(name.get("value").asText() + " @" + context.getPreferredLanguage());
                    }
                }
            }
        }
        return taxonImage;
    }

    private boolean valueExists(JsonNode name) {
        return name != null && name.has("value") && !name.isNull();
    }

}
