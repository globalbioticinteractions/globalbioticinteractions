package org.eol.globi.taxon;

import org.apache.commons.lang.CharSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.LanguageCodeLookup;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GBIFService implements PropertyEnricher {

    private static final Log LOG = LogFactory.getLog(GBIFService.class);

    @Override
    public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
        // see http://www.gbif.org/developer/species
        Map<String, String> enriched = new HashMap<String, String>(properties);
        String externalId = properties.get(PropertyAndValueDictionary.EXTERNAL_ID);
        if (StringUtils.startsWith(externalId, TaxonomyProvider.GBIF.getIdPrefix())) {
            enrichWithExternalId(enriched, externalId);
        }
        return enriched;
    }

    protected void enrichWithExternalId(Map<String, String> enriched, String externalId) throws PropertyEnricherException {
        try {
            String gbifSpeciesId = StringUtils.replace(externalId, TaxonomyProvider.GBIF.getIdPrefix(), "");
            String response = HttpUtil.getContent("http://api.gbif.org/v1/species/" + gbifSpeciesId);
            JsonNode jsonNode = new ObjectMapper().readTree(response);
            addTaxonNode(enriched, jsonNode);

            LanguageCodeLookup languageCodeLookup = new LanguageCodeLookup();
            String vernaculars = HttpUtil.getContent("http://api.gbif.org/v1/species/" + gbifSpeciesId + "/vernacularNames");
            jsonNode = new ObjectMapper().readTree(vernaculars);
            JsonNode results = jsonNode.get("results");
            List<String> commonNames = new ArrayList<String>();
            if (results != null && results.isArray()) {
                for (JsonNode result : results) {
                    if (result.has("vernacularName") && result.has("language")) {
                        JsonNode preferred = result.get("preferred");
                        if (preferred == null || (preferred.isBoolean() && preferred.asBoolean())) {
                            String commonName = result.get("vernacularName").asText();
                            String language = result.get("language").asText();
                            String shortCode = languageCodeLookup.lookupLanguageCodeFor(language);
                            if (StringUtils.isNotBlank(commonName) && StringUtils.isNotBlank(shortCode)) {
                                commonNames.add(commonName + " @" + shortCode);
                            }

                        }
                    }
                }
            }
            enriched.put(PropertyAndValueDictionary.COMMON_NAMES, StringUtils.join(commonNames, CharsetConstant.SEPARATOR));
        } catch (IOException e) {
            throw new PropertyEnricherException("failed to lookup [" + externalId + "]", e);
        }

    }

    protected void addTaxonNode(Map<String, String> enriched, JsonNode jsonNode) {
        String externalId;
        externalId = jsonNode.has("key") ? (TaxonomyProvider.GBIF.getIdPrefix() + jsonNode.get("key").asText()) : "";
        String name = jsonNode.has("canonicalName") ? jsonNode.get("canonicalName").asText() : "";
        String rank = jsonNode.has("rank") ? jsonNode.get("rank").asText().toLowerCase() : "";

        enriched.put(PropertyAndValueDictionary.NAME, name);
        enriched.put(PropertyAndValueDictionary.RANK, rank);
        enriched.put(PropertyAndValueDictionary.EXTERNAL_ID, externalId);

        String[] pathLabels = new String[]{
                "kingdom",
                "phylum",
                "class",
                "order",
                "family",
                "genus",
                "species"};

        String[] pathIdLabels = new String[]{
                "kingdomKey",
                "phylumKey",
                "classKey",
                "orderKey",
                "familyKey",
                "genusKey",
                "speciesKey"};
        List<String> ids = collect(jsonNode, pathIdLabels, TaxonomyProvider.GBIF.getIdPrefix());
        enriched.put(PropertyAndValueDictionary.PATH_IDS, toString(ids));

        List<String> path = collect(jsonNode, pathLabels);
        enriched.put(PropertyAndValueDictionary.PATH, toString(path));

        List<String> pathNames = Arrays.asList(pathLabels);
        enriched.put(PropertyAndValueDictionary.PATH_NAMES, toString(pathNames));
    }

    private List<String> collect(JsonNode jsonNode, String[] pathIdLabels) {
        return collect(jsonNode, pathIdLabels, "");
    }

    private List<String> collect(JsonNode jsonNode, String[] pathIdLabels, String prefix) {
        List<String> ids = new ArrayList<String>();
        for (String pathIdLabel : pathIdLabels) {
            ids.add(jsonNode.has(pathIdLabel) ? (prefix + jsonNode.get(pathIdLabel).asText()) : "");
        }
        return ids;
    }

    protected String toString(List<String> ids) {
        return StringUtils.join(ids, CharsetConstant.SEPARATOR);
    }

    public void shutdown() {

    }
}
