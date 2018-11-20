package org.eol.globi.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.ImageSearch;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;
import org.springframework.util.StringUtils;

import java.io.IOException;

public class WikiDataImageSearch implements ImageSearch {

    private static Log LOG = LogFactory.getLog(WikiDataImageSearch.class);

    @Override
    public TaxonImage lookupImageForExternalId(String externalId) throws IOException {
        return StringUtils.startsWithIgnoreCase(externalId, TaxonomyProvider.WIKIDATA.getIdPrefix())
                ? requestImage(externalId)
                : null;
    }

    private TaxonImage requestImage(String externalId) throws IOException {
        TaxonImage taxonImage = null;
        String wikiDataId = StringUtils.replace(externalId, TaxonomyProvider.WIKIDATA.getIdPrefix(), "");
        String url = "https://query.wikidata.org/sparql?query=SELECT%20%3Fitem%20%3Fpic%20WHERE%20%7B%20wd%3A"
                + wikiDataId
                + "%20wdt%3AP18%20%3Fpic%20%7D%20limit%201";
        LOG.info("requesting image [" + url + "]");
        HttpGet httpGet = HttpUtil.httpGetJson(url);
        String jsonString = HttpUtil.executeAndRelease(httpGet, HttpUtil.getFailFastHttpClient());
        JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
        if (jsonNode.has("results")) {
            JsonNode results = jsonNode.get("results");
            if (results.has("bindings")) {
                JsonNode bindings = results.get("bindings");
                for (JsonNode binding : bindings) {
                    taxonImage = new TaxonImage();
                    taxonImage.setInfoURL(ExternalIdUtil.urlForExternalId(externalId));
                    JsonNode pic = binding.get("pic");
                    if (pic.has("value")) {
                        taxonImage.setThumbnailURL(pic.get("value").asText() + "?width=100");
                    }
                }
            }
        }
        return taxonImage;
    }
}
