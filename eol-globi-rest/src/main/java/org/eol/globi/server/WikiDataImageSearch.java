package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.WordUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.ImageSearch;
import org.eol.globi.service.SearchContext;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
    public static final Map<TaxonomyProvider, String> PROVIDER_TO_WIKIDATA = new TreeMap<TaxonomyProvider, String>() {{
        put(TaxonomyProvider.ITIS, "P815");
        put(TaxonomyProvider.NCBI, "P685");
        put(TaxonomyProvider.EOL, "P830");
        put(TaxonomyProvider.EOL_V2, "P830");
        put(TaxonomyProvider.WORMS, "P850");
        put(TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA, "P5055");
        put(TaxonomyProvider.FISHBASE_SPECCODE, "P938");
        put(TaxonomyProvider.SEALIFEBASE_SPECCODE, "P6018");
        put(TaxonomyProvider.GBIF, "P846");
        put(TaxonomyProvider.INATURALIST_TAXON, "P3151");
        put(TaxonomyProvider.NBN, "P3240");
        put(TaxonomyProvider.MSW, "P959");
    }};

    public static final Map<String, TaxonomyProvider> WIKIDATA_TO_PROVIDER = new TreeMap<String, TaxonomyProvider>() {{
        put("P815", TaxonomyProvider.ITIS);
        put("P685", TaxonomyProvider.NCBI);
        put("P830", TaxonomyProvider.EOL);
        put("P850", TaxonomyProvider.WORMS);
        put("P5055", TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA);
        put("P938", TaxonomyProvider.FISHBASE_SPECCODE);
        put("P6018", TaxonomyProvider.SEALIFEBASE_SPECCODE);
        put("P846", TaxonomyProvider.GBIF);
        put("P3151", TaxonomyProvider.INATURALIST_TAXON);
        put("P3240", TaxonomyProvider.NBN);
        put("P959", TaxonomyProvider.MSW);
    }};

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
            String jsonString = executeQuery(sparql);
            taxonImage = parseWikidataResult(externalId, taxonImage, jsonString, context);

        } catch (URISyntaxException e) {
            throw new IOException("marlformed uri", e);
        }
        return taxonImage;
    }

    private String executeQuery(String sparql) throws URISyntaxException, IOException {
        URI url = new URI("https", "query.wikidata.org", "/sparql", "query=" + sparql, null);
        LOG.info("executing sparql [" + url + "]");
        HttpGet httpGet = HttpUtil.httpGetJson(url);
        return HttpUtil.executeAndRelease(httpGet, HttpUtil.getFailFastHttpClient());
    }

    public List<String> findTaxonIdProviders() throws IOException, URISyntaxException {

        List<String> providers = new ArrayList<>();
        String sparlql = "SELECT ?scheme ?urlScheme ?idRegex WHERE { " +
                "?scheme wdt:P31 wd:Q42396390 . " +
                //"?scheme wdt:P1630 ?urlScheme . " +
                //"?scheme wdt:P1793 ?idRegex . " +
                "} ";
        final String jsonString = executeQuery(sparlql);
        JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
        if (jsonNode.has("results")) {
            JsonNode results = jsonNode.get("results");
            if (results.has("bindings")) {
                JsonNode bindings = results.get("bindings");
                for (JsonNode binding : bindings) {
                    JsonNode scheme = binding.get("scheme");
                    if (valueExists(scheme)) {
                        String taxonProvider = scheme.get("value").asText();
                        providers.add(StringUtils.replace(taxonProvider, "http://www.wikidata.org/entity/", ""));
                    }
                }
            }
        }
        return providers;
    }

    public Map<TaxonomyProvider, String> findRelatedTaxonIds(String externalId) throws IOException, URISyntaxException {
        Map<TaxonomyProvider, String> relatedIds = new TreeMap<>();
        String sparql = "SELECT ?scheme ?urlScheme ?idRegex WHERE { " +
                "?scheme wdt:P31 wd:Q42396390 . " +
                //"?scheme wdt:P1630 ?urlScheme . " +
                //"?scheme wdt:P1793 ?idRegex . " +
                "} ";
        final TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(externalId);
        if (taxonomyProvider != null) {
            final String taxonId = ExternalIdUtil.stripPrefix(taxonomyProvider, externalId);
            String whereClause =
                    TaxonomyProvider.WIKIDATA.equals(taxonomyProvider)
                            ? wdTaxonWhereClause(taxonId)
                            : nonWdTaxonWhereClause(taxonomyProvider, taxonId);

            sparql =
                    "SELECT ?wdTaxonId ?taxonScheme ?taxonId WHERE {\n" + whereClause +
                            "  ?taxonSchemeEntity wikibase:directClaim ?taxonScheme .\n" +
                            "  ?taxonSchemeEntity wdt:P31 wd:Q42396390 .\n" +
                            "}";

            final String jsonString = executeQuery(sparql);
            JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
            if (jsonNode.has("results")) {
                JsonNode results = jsonNode.get("results");
                if (results.has("bindings")) {
                    JsonNode bindings = results.get("bindings");
                    for (JsonNode binding : bindings) {
                        JsonNode wdTaxonId = binding.get("wdTaxonId");
                        if (valueExists(wdTaxonId)) {
                            relatedIds.putIfAbsent(TaxonomyProvider.WIKIDATA, ExternalIdUtil.stripPrefix(TaxonomyProvider.WIKIDATA, wdTaxonId.get("value").asText()));
                        }

                        JsonNode scheme = binding.get("taxonScheme");
                        if (valueExists(scheme)) {
                            String taxonProvider = scheme.get("value").asText();
                            final String wdProviderPropertyId = replace(taxonProvider, "http://www.wikidata.org/prop/direct/", "");
                            final TaxonomyProvider taxonomyProvider1 = WIKIDATA_TO_PROVIDER.get(wdProviderPropertyId);
                            if (taxonomyProvider1 != null) {
                                String linkedTaxonId = binding.get("taxonId").get("value").asText();
                                relatedIds.putIfAbsent(taxonomyProvider1, linkedTaxonId);
                            }
                        }
                    }
                }
            }
        }
        return relatedIds;
    }

    public String nonWdTaxonWhereClause(TaxonomyProvider taxonomyProvider, String taxonId) {
        return "?wdTaxonId wdt:" + PROVIDER_TO_WIKIDATA.get(taxonomyProvider) + " \"" + taxonId + "\" .\n" +
                "?wdTaxonId ?taxonScheme ?taxonId .\n";
    }

    public String wdTaxonWhereClause(String taxonId) {
        return "bind ( wd:" + taxonId + " as ?wdTaxonId )\n" +
                "wd:" + taxonId + " ?taxonScheme ?taxonId .\n";
    }

    static String createSparqlQuery(String externalId, String preferredLanguage) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(externalId);

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
        } else if (taxonomyProvider != null && PROVIDER_TO_WIKIDATA.containsKey(taxonomyProvider)) {
            String taxonId = replace(externalId, taxonomyProvider.getIdPrefix(), "");
            query = "SELECT ?item ?pic ?name ?wdpage WHERE {\n" +
                    "  ?wdpage wdt:P18 ?pic .\n" +
                    "  ?wdpage wdt:" + PROVIDER_TO_WIKIDATA.get(taxonomyProvider) + " \"" + taxonId + "\" .\n" +
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
