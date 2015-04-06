package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.TaxonomyProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalIdUtil {
    public static String infoURLForExternalId(String externalId) {
        String url = null;
        if (externalId != null) {
            for (Map.Entry<String, String> idPrefixToUrlPrefix : getURLPrefixMap().entrySet()) {
                String idPrefix = idPrefixToUrlPrefix.getKey();
                if (StringUtils.startsWith(externalId, idPrefix)) {
                    url = idPrefixToUrlPrefix.getValue() + externalId.replaceAll(idPrefix, "");
                    String suffix = getURLSuffixMap().get(idPrefix);
                    if (StringUtils.isNotBlank(suffix)) {
                        url = url + suffix;
                    }
                }
                if (url != null) {
                    break;
                }
            }
        }
        return url;
    }

    public static Map<String, String> getURLPrefixMap() {
        return new HashMap<String, String>() {{
            put(TaxonomyProvider.ID_PREFIX_EOL, "http://eol.org/pages/");
            put(TaxonomyProvider.ID_PREFIX_WORMS, "http://www.marinespecies.org/aphia.php?p=taxdetails&id=");
            put(TaxonomyProvider.ID_PREFIX_ENVO, "http://purl.obolibrary.org/obo/ENVO_");
            put(TaxonomyProvider.ID_PREFIX_WIKIPEDIA, "http://wikipedia.org/wiki/");
            put(TaxonomyProvider.ID_PREFIX_GULFBASE, "http://gulfbase.org/biogomx/biospecies.php?species=");
            put(TaxonomyProvider.ID_PREFIX_GAME, "http://research.myfwc.com/game/Survey.aspx?id=");
            put(TaxonomyProvider.ID_CMECS, "http://cmecscatalog.org/classification/aquaticSetting/");
            put(TaxonomyProvider.ID_BIO_INFO_REFERENCE, "http://bioinfo.org.uk/html/b");
            put(TaxonomyProvider.ID_PREFIX_GBIF, "http://www.gbif.org/species/");
            put(TaxonomyProvider.ID_PREFIX_INATURALIST, "http://www.inaturalist.org/observations/");
            put(TaxonomyProvider.ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY, "http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/afd/taxa/");
            put(TaxonomyProvider.ID_PREFIX_NBN, "https://data.nbn.org.uk/Taxa/");
            put(TaxonomyProvider.ID_PREFIX_DOI, "http://dx.doi.org/");
            put(TaxonomyProvider.ID_PREFIX_HTTP, TaxonomyProvider.ID_PREFIX_HTTP);
        }};
    }

    public static Map<String, String> getURLSuffixMap() {
        return new HashMap<String, String>() {{
            put(TaxonomyProvider.ID_BIO_INFO_REFERENCE, ".htm");
        }};
    }

    public static boolean isSupported(String externalId) {
        boolean supported = false;
        if (StringUtils.isNotBlank(externalId)) {
            for (TaxonomyProvider prefix : TaxonomyProvider.values()) {
                if (StringUtils.startsWith(externalId, prefix.getIdPrefix())) {
                    supported = true;
                }
            }
        }
        return supported;
    }

    public static String getUrlFromExternalId(String result) {
        String externalId = null;
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(result);
            JsonNode data = jsonNode.get("data");
            if (data != null) {
                for (JsonNode row : data) {
                    for (JsonNode cell : row) {
                        externalId = cell.asText();
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return buildJsonUrl(infoURLForExternalId(externalId));
    }

    public static String buildJsonUrl(String url) {
        return StringUtils.isBlank(url) ? "{}" : "{\"url\":\"" + url + "\"}";
    }

    public static String toCitation(String contributor, String description, String publicationYear) {
        String[] array = {contributor, publicationYear, description};
        List<String> nonBlanks = new ArrayList<String>();
        for (String string : array) {
            if (StringUtils.isNotBlank(string)) {
                nonBlanks.add(string);
            }
        }
        return StringUtils.join(nonBlanks, ". ");
    }
}
