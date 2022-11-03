package org.globalbioticinteractions.wikidata;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.service.ImageSearch;
import org.eol.globi.service.SearchContext;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.split;

public class WikiDataImageSearch implements ImageSearch {

    @Override
    public TaxonImage lookupImageForExternalId(String externalId) throws IOException {
        return lookupImageForExternalId(externalId, () -> "en");
    }

    @Override
    public TaxonImage lookupImageForExternalId(String externalId, SearchContext context) throws IOException {
        String sparqlQuery = WikidataUtil.createSparqlQuery(externalId, context.getPreferredLanguage());
        return StringUtils.isBlank(sparqlQuery)
                ? null
                : executeQuery(externalId, context, sparqlQuery);
    }

    private TaxonImage executeQuery(String externalId, SearchContext context, String sparql) throws IOException {
        TaxonImage taxonImage = null;
        try {
            String jsonString = WikidataUtil.executeQuery(sparql);
            taxonImage = parseWikidataResult(externalId, taxonImage, jsonString, context);

        } catch (URISyntaxException e) {
            throw new IOException("marlformed uri", e);
        }
        return taxonImage;
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
                    if (WikidataUtil.valueExists(wdPage)) {
                        String pageUrl = wdPage.get("value").asText();
                        taxonImage.setInfoURL(pageUrl);
                    } else {
                        taxonImage.setInfoURL(ExternalIdUtil.urlForExternalId(externalId));
                    }

                    JsonNode pic = binding.get("pic");
                    if (WikidataUtil.valueExists(pic)) {
                        taxonImage.setThumbnailURL(replace(pic.get("value").asText(), "http:", "https:") + "?width=100");
                    }
                    JsonNode name = binding.get("name");
                    if (WikidataUtil.valueExists(name)) {
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

}
