package org.eol.globi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.commons.lang3.StringUtils.replace;

public final class WikidataUtil {

    public static final Map<TaxonomyProvider, String> PROVIDER_TO_WIKIDATA = new TreeMap<TaxonomyProvider, String>() {{
        put(TaxonomyProvider.OPEN_TREE_OF_LIFE, "P9157");
        put(TaxonomyProvider.BOLD_TAXON, "P3606");
        put(TaxonomyProvider.INDEX_FUNGORUM, "P1391");
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
        put(TaxonomyProvider.PLAZI, "P1992");
        put(TaxonomyProvider.CATALOGUE_OF_LIFE, "P10585");
        put(TaxonomyProvider.WORLD_OF_FLORA_ONLINE, "P7715");
    }};
    public static final Map<String, TaxonomyProvider> WIKIDATA_TO_PROVIDER = new TreeMap<String, TaxonomyProvider>() {{
        put("P9157", TaxonomyProvider.OPEN_TREE_OF_LIFE);
        put("P3606", TaxonomyProvider.BOLD_TAXON);
        put("P1391", TaxonomyProvider.INDEX_FUNGORUM);
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
        put("P1992", TaxonomyProvider.PLAZI);
        put("P7715", TaxonomyProvider.WORLD_OF_FLORA_ONLINE);
        put("P10585", TaxonomyProvider.CATALOGUE_OF_LIFE);
    }};

    public static String executeQuery(String sparql) throws URISyntaxException, IOException {
        URI url = new URI("https", "query.wikidata.org", "/sparql", "query=" + sparql, null);
        HttpGet httpGet = HttpUtil.httpGetJson(url);
        final HttpClient failFastHttpClient = HttpUtil.getFailFastHttpClient();
        return HttpUtil.executeAndRelease(httpGet, failFastHttpClient);
    }

    public static List<String> findTaxonIdProviders() throws IOException, URISyntaxException {

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
        return Collections.unmodifiableList(new ArrayList<String>(providers) {{
            // manually add World of Flora Online;
            // see related https://github.com/globalbioticinteractions/nomer/issues/102
            add("P7715");
        }});
    }

    public static List<Taxon> findRelatedTaxonIds(String externalId) throws IOException, URISyntaxException {
        List<Taxon> relatedIds = new ArrayList<>();
        String sparql;
        final TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(externalId);
        if (taxonomyProvider != null) {
            final String taxonId = ExternalIdUtil.stripPrefix(taxonomyProvider, externalId);
            String whereClause =
                    TaxonomyProvider.WIKIDATA.equals(taxonomyProvider)
                            ? wdTaxonWhereClause(taxonId)
                            : nonWdTaxonWhereClause(taxonomyProvider, taxonId);

            sparql =
                    "SELECT ?wdTaxonId ?taxonScheme ?taxonId ?wdTaxonName WHERE {\n" + whereClause +
                            "  ?taxonSchemeEntity wikibase:directClaim ?taxonScheme .\n" +
                            "  ?taxonSchemeEntity wdt:P31 wd:Q42396390 .\n" +
                            "  OPTIONAL { ?wdTaxonId wdt:P225 ?wdTaxonName . }\n" +
                            "}";

            final String jsonString = executeQuery(sparql);
            JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
            if (jsonNode.has("results")) {
                JsonNode results = jsonNode.get("results");
                if (results.has("bindings")) {
                    JsonNode bindings = results.get("bindings");
                    addWikidataTaxon(relatedIds, bindings);
                    addLinkedTaxa(relatedIds, bindings);

                }
            }
        }
        return relatedIds;
    }

    public static void addLinkedTaxa(List<Taxon> relatedIds, JsonNode bindings) {
        for (JsonNode binding : bindings) {
            JsonNode scheme = binding.get("taxonScheme");
            if (valueExists(scheme)) {
                String taxonProvider = scheme.get("value").asText();
                final String wdProviderPropertyId = replace(taxonProvider, "http://www.wikidata.org/prop/direct/", "");
                final TaxonomyProvider taxonomyProvider1 = WIKIDATA_TO_PROVIDER.get(wdProviderPropertyId);
                if (taxonomyProvider1 != null) {
                    TaxonImpl taxon = new TaxonImpl();
                    String linkedTaxonId = binding.get("taxonId").get("value").asText();
                    taxon.setExternalId(taxonomyProvider1.getIdPrefix() + linkedTaxonId);
                    populateNameAndRank(binding, taxon);
                    relatedIds.add(taxon);
                }
            }
        }
    }

    private static void addWikidataTaxon(List<Taxon> relatedIds, JsonNode bindings) {
        if (bindings.size() > 0) {
            TaxonImpl taxon1 = new TaxonImpl();
            final JsonNode binding = bindings.get(0);
            JsonNode wdTaxonId = binding.get("wdTaxonId");
            if (valueExists(wdTaxonId)) {
                final String id = wdTaxonId.get("value").asText();
                final String wikidataId = ExternalIdUtil.stripPrefix(TaxonomyProvider.WIKIDATA, id);
                taxon1.setExternalId(TaxonomyProvider.WIKIDATA.getIdPrefix() + wikidataId);
            }
            populateNameAndRank(binding, taxon1);
            relatedIds.add(taxon1);
        }
    }

    private static void populateNameAndRank(JsonNode binding, TaxonImpl taxon) {
        JsonNode name = binding.get("wdTaxonName");
        if (valueExists(name)) {
            final String name1 = name.get("value").asText();
            taxon.setName(name1);
            taxon.setPath(name1);
        }
    }

    public static String nonWdTaxonWhereClause(TaxonomyProvider taxonomyProvider, String taxonId) {
        return "?wdTaxonId wdt:" + PROVIDER_TO_WIKIDATA.get(taxonomyProvider) + " \"" + taxonId + "\" .\n" +
                "?wdTaxonId ?taxonScheme ?taxonId .\n";
    }

    public static String wdTaxonWhereClause(String taxonId) {
        return "bind ( wd:" + taxonId + " as ?wdTaxonId )\n" +
                "wd:" + taxonId + " ?taxonScheme ?taxonId .\n";
    }

    public static String createSparqlQuery(String externalId, String preferredLanguage) {
        TaxonomyProvider taxonomyProvider = ExternalIdUtil.taxonomyProviderFor(externalId);

        String query = null;

        if (TaxonomyProvider.WIKIDATA.equals(taxonomyProvider)) {
            String wikiDataId = replace(externalId, TaxonomyProvider.WIKIDATA.getIdPrefix(), "");
            query = "SELECT ?pic ?name ?nameLabel WHERE {\n" +
                    "  OPTIONAL { wd:" + wikiDataId + " wdt:P18 ?pic . }\n" +
                    "  SERVICE wikibase:label {\n"+
                    "    bd:serviceParam wikibase:language \"" + preferredLanguage + "\".\n" +
                    "    wd:" + wikiDataId + " wdt:P1843 ?name .\n" +
                    "  }\n" +
                    "} limit 1";
        } else if (taxonomyProvider != null && PROVIDER_TO_WIKIDATA.containsKey(taxonomyProvider)) {
            String taxonId = replace(externalId, taxonomyProvider.getIdPrefix(), "");
            query = "SELECT ?pic ?name ?wdpage WHERE {\n" +
                    "  ?wdpage wdt:" + PROVIDER_TO_WIKIDATA.get(taxonomyProvider) + " \"" + taxonId + "\" .\n" +
                    "  OPTIONAL { ?wdpage wdt:P18 ?pic . }\n" +
                    "  SERVICE wikibase:label {\n" +
                    "   bd:serviceParam wikibase:language \"" + preferredLanguage + "\" .\n" +
                    "   ?wdpage wdt:P1843 ?name .\n" +
                    "  }\n" +
                    "} limit 1";
        }
        return query;
    }

    public static boolean valueExists(JsonNode name) {
        return name != null && name.hasNonNull("value");
    }
}
