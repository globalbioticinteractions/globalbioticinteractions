package org.globalbioticinteractions.wikidata;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.text.WordUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.ImageSearch;
import org.eol.globi.service.SearchContext;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.split;

public class WikiDataImageSearch implements ImageSearch {

    private static final Logger LOG = LoggerFactory.getLogger(WikiDataImageSearch.class);

    public static void enrichWithThumbnailAndCommonNamesIfAvailable(
            JsonNode jsonNode,
            String entityId,
            String preferredLanguage,
            TaxonImage image) {
        JsonNode entity = jsonNode.at("/entities/" + entityId);

        List<String> commonNames = new ArrayList<>();
        URI imageUrl = null;
        if (!entity.isMissingNode()) {
            appendNames(entity, "/labels/", preferredLanguage, commonNames);
            appendNames(entity, "/aliases/", preferredLanguage, commonNames);
        }
        image.setCommonName(commonNames
                .stream()
                .map(n -> n + " @" + preferredLanguage)
                .collect(Collectors.joining(CharsetConstant.SEPARATOR))
        );

        String scientificName = getClaimValue(entity, "P225");
        image.setScientificName(scientificName);

        String imageFilename = getClaimValue(entity, "P18");

        if (StringUtils.isNotBlank(imageFilename)) {
            try {
                imageUrl = new URI("https", "commons.wikimedia.org", "/w/index.php", "title=Special:Redirect/file/"
                        + StringUtils.replaceChars(imageFilename, " ", "_") + "&width=100", null);
                image.setThumbnailURL(imageUrl.toString());
            } catch (URISyntaxException e) {
                //
            }
        }
    }

    private static String getClaimValue(JsonNode entity, String propertyType) {
        String imageFilename = null;
        JsonNode hasDepiction = entity.at("/claims/" + propertyType);
        JsonNode depictionEntry = (hasDepiction.isArray() && !hasDepiction.isEmpty())
                ? hasDepiction.get(0)
                : hasDepiction;
        JsonNode filename = depictionEntry.at("/mainsnak/datavalue/value");
        if (filename.isTextual()) {
            imageFilename = filename.asText();
        }
        return imageFilename;
    }

    public static void appendNames(JsonNode entity, String s1, String preferredLanguage, List<String> commonNames) {
        JsonNode labels = entity.at(s1 + preferredLanguage);
        if (labels.has("value")) {
            appendName(commonNames, labels);
        } else {
            for (JsonNode label : labels) {
                appendName(commonNames, label);
            }
        }
    }

    public static void appendName(List<String> commonNames, JsonNode labels) {
        JsonNode at = labels.at("/value");
        if (at.isTextual()) {
            commonNames.add(at.asText());
        }
    }

    public static String getWikidataEntry(String entityId, String preferredLanguage) throws IOException {
        String query = "action=wbgetentities&ids=" + entityId + "&languagefallback=en&languages=" + preferredLanguage + "&format=json&props=aliases|labels|claims";
        URI request;
        try {
            request = new URI("https", "www.wikidata.org", "/w/api.php", query, null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            LOG.info("request [" + request + "] sent ...");
            HttpGet httpGet = HttpUtil.httpGetJson(request);
            return HttpUtil.executeAndRelease(httpGet, HttpUtil.getFailFastHttpClient());
        } finally {
            stopWatch.stop();
            LOG.info("request [" + request + "] completed in " + stopWatch.getTime(TimeUnit.MILLISECONDS) + "ms");
        }
    }


    @Override
    public TaxonImage lookupImageForExternalId(String externalId) throws IOException {
        return lookupImageForExternalId(externalId, () -> "en");
    }

    @Override
    public TaxonImage lookupImageForExternalId(String externalId, SearchContext context) throws IOException {
        String preferredLanguage = context.getPreferredLanguage();
        if (TaxonomyProvider.WIKIDATA.equals(ExternalIdUtil.taxonomyProviderFor(externalId))) {
            String entityId = ExternalIdUtil.stripPrefix(TaxonomyProvider.WIKIDATA, externalId);
            String s = WikiDataImageSearch.getWikidataEntry(entityId, preferredLanguage);
            TaxonImage taxonImage = new TaxonImage();
            taxonImage.setInfoURL(ExternalIdUtil.urlForExternalId(externalId));
            enrichWithThumbnailAndCommonNamesIfAvailable(new ObjectMapper().readTree(s), entityId, preferredLanguage, taxonImage);
            return taxonImage;
        } else {
            String sparqlQuery = WikidataUtil.createSparqlQuery(externalId, preferredLanguage);
            return StringUtils.isBlank(sparqlQuery)
                    ? null
                    : executeQuery(externalId, context, sparqlQuery);
        }
    }

    private TaxonImage executeQuery(String externalId, SearchContext context, String sparql) throws IOException {
        TaxonImage taxonImage;
        try {
            String jsonString = WikidataUtil.executeQuery(sparql);
            taxonImage = parseWikidataResult(externalId, jsonString, context);

        } catch (URISyntaxException e) {
            throw new IOException("marlformed uri", e);
        }
        return taxonImage;
    }

    private TaxonImage parseWikidataResult(String externalId, String jsonString, SearchContext context) throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
        return getTaxonImage(externalId, context, jsonNode);
    }

    private static JsonNode getBindingValue(JsonNode jsonNode, String bindingName) {
        JsonNode bindingValue = null;
        if (jsonNode.has("results")) {
            JsonNode results = jsonNode.get("results");
            if (results.has("bindings")) {
                JsonNode bindings = results.get("bindings");
                for (JsonNode binding : bindings) {
                    bindingValue = binding.get(bindingName);
                }
            }
        }
        return bindingValue;
    }

    private static TaxonImage getTaxonImage(String externalId, SearchContext context, JsonNode jsonNode) {
        JsonNode wdPage = getBindingValue(jsonNode, "wdpage");
        TaxonImage taxonImage = new TaxonImage();
        String pageUrl = null;
        if (WikidataUtil.valueExists(wdPage)) {
            pageUrl = wdPage.get("value").asText();
            taxonImage.setInfoURL(pageUrl);
        } else {
            pageUrl = ExternalIdUtil.urlForExternalId(externalId);
        }
        taxonImage.setInfoURL(pageUrl);

        JsonNode pic = getBindingValue(jsonNode, "pic");
        if (WikidataUtil.valueExists(pic)) {
            taxonImage.setThumbnailURL(replace(pic.get("value").asText(), "http:", "https:") + "?width=100");
        }
        JsonNode name = getBindingValue(jsonNode, "name");
        if (WikidataUtil.valueExists(name)) {
            String value = name.get("value").asText();
            String[] split = split(value, ",");
            List<String> names = Stream
                    .of(split)
                    .map(String::trim)
                    .map(WordUtils::capitalizeFully)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            taxonImage.setCommonName(join(names, ", ") + " @" + context.getPreferredLanguage());
        }
        return taxonImage;
    }

}
