package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.WordUtils;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.ImageSearch;
import org.eol.globi.service.SearchContext;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.split;

public class WikiDataImageSearch implements ImageSearch {

    private static Log LOG = LogFactory.getLog(WikiDataImageSearch.class);

    @Override
    public TaxonImage lookupImageForExternalId(String externalId) throws IOException {
        return lookupImageForExternalId(externalId, () -> "en");
    }

    @Override
    public TaxonImage lookupImageForExternalId(String externalId, SearchContext context) throws IOException {
        String sparqlQuery = createSparqlQuery(externalId, context.getPreferredLanguage());
        return StringUtils.isBlank(sparqlQuery)
                ? null
                : executeQuery(externalId, context, sparqlQuery);
    }

    private TaxonImage executeQuery(String externalId, SearchContext context, String sparql) throws IOException {
        TaxonImage taxonImage = null;
        try {
            URI url = new URI("https", "query.wikidata.org", "/sparql", "query=" + sparql, null);
            LOG.info("requesting image [" + url + "]");
            HttpGet httpGet = HttpUtil.httpGetJson(url);
            String jsonString = HttpUtil.executeAndRelease(httpGet, HttpUtil.getFailFastHttpClient());
            taxonImage = parseWikidataResult(externalId, taxonImage, jsonString, context);

        } catch (URISyntaxException e) {
            throw new IOException("marlformed uri", e);
        }
        return taxonImage;
    }

    String createSparqlQuery(String externalId, String preferredLanguage) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(externalId);
        Map<TaxonomyProvider, String> nonWdProviders = new TreeMap<TaxonomyProvider, String>() {{
            put(TaxonomyProvider.ITIS, "P815");
            put(TaxonomyProvider.NCBI, "P685");
            put(TaxonomyProvider.EOL, "P830");
            put(TaxonomyProvider.WORMS, "P850");
            put(TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA, "P5055");
            put(TaxonomyProvider.FISHBASE_SPECCODE, "P938");
            put(TaxonomyProvider.SEALIFEBASE_SPECCODE, "P6018");
            put(TaxonomyProvider.GBIF, "P846");
            put(TaxonomyProvider.INATURALIST_TAXON, "P3151");
        }};

        String query = null;

        if (TaxonomyProvider.WIKIDATA.equals(taxonomyProvider)) {
            String wikiDataId = replace(externalId, TaxonomyProvider.WIKIDATA.getIdPrefix(), "");

            query = "SELECT ?item ?pic ?name WHERE {\n" +
                    "  wd:" + wikiDataId + " wdt:P18 ?pic .\n" +
                    "  SERVICE wikibase:label {\n" +
                    "    bd:serviceParam wikibase:language \"" + preferredLanguage + "\" .\n" +
                    "    wd:" + wikiDataId + " wdt:P1843 ?name .\n" +
                    "  }\n" +
                    "} limit 1";
        } else if (taxonomyProvider != null && nonWdProviders.containsKey(taxonomyProvider)) {
            String taxonId = replace(externalId, taxonomyProvider.getIdPrefix(), "");
            query = "SELECT ?item ?pic ?name ?wdpage WHERE {\n" +
                    "  ?wdpage wdt:P18 ?pic .\n" +
                    "  ?wdpage wdt:" + nonWdProviders.get(taxonomyProvider) + " \"" + taxonId + "\" .\n" +
                    "  SERVICE wikibase:label {\n" +
                    "   bd:serviceParam wikibase:language \"" + preferredLanguage + "\" .\n" +
                    "   ?wdpage wdt:P1843 ?name .\n" +
                    "  }\n" +
                    "} limit 1";
        }
        return query;
    }

    private TaxonImage parseWikidataResult(String externalId, TaxonImage taxonImage, String jsonString, SearchContext context) throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
        if (jsonNode.has("results")) {
            JsonNode results = jsonNode.get("results");
            if (results.has("bindings")) {
                JsonNode bindings = results.get("bindings");
                for (JsonNode binding : bindings) {
                    taxonImage = new TaxonImage();
                    JsonNode wdPage = binding.get("wdpage");
                    if (valueExists(wdPage)) {
                        String pageUrl = wdPage.get("value").asText();
                        taxonImage.setInfoURL(pageUrl);
                    } else {
                        taxonImage.setInfoURL(ExternalIdUtil.urlForExternalId(externalId));
                    }

                    JsonNode pic = binding.get("pic");
                    if (valueExists(pic)) {
                        taxonImage.setThumbnailURL(replace(pic.get("value").asText(), "http:", "https:") + "?width=100");
                    }
                    JsonNode name = binding.get("name");
                    if (valueExists(name)) {
                        String value = name.get("value").asText();
                        String[] split = split(value, ",");
                        List<String> names = Stream
                                .of(split)
                                .map(String::trim)
                                .map(WordUtils::capitalizeFully)
                                .distinct()
                                .collect(Collectors.toList());

                        taxonImage.setCommonName(join(names, ", ") + " @" + context.getPreferredLanguage());
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
