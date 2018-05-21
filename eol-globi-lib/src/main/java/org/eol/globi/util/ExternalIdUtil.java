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
    public static String urlForExternalId(String externalId) {
        String url = null;
        if (externalId != null) {
            for (Map.Entry<String, String> idPrefixToUrlPrefix : getURLPrefixMap().entrySet()) {
                String idPrefix = idPrefixToUrlPrefix.getKey();
                if (StringUtils.startsWith(externalId, idPrefix)) {
                    if (isIRMNG(idPrefix)) {
                        url = urlForIRMNG(externalId, idPrefix);
                    } else {
                        url = idPrefixToUrlPrefix.getValue() + externalId.replaceAll(idPrefix, "");
                    }
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

    public static boolean isIRMNG(String idPrefix) {
        return StringUtils.equals(TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA.getIdPrefix(), idPrefix);
    }

    public static String urlForIRMNG(String externalId, String idPrefix) {
        String url;
        final String id = externalId.replaceAll(idPrefix, "");
        if (id.length() == 6) {
            url = "http://www.marine.csiro.au/mirrorsearch/ir_search.list_genera?fam_id=" + id;
        } else if (id.length() == 7) {
            url = "http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?gen_id=" + id;
        } else {
            url = "http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?sp_id=" + id;
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
            put(TaxonomyProvider.ID_PREFIX_GAME, "https://public.myfwc.com/FWRI/GAME/Survey.aspx?id=");
            put(TaxonomyProvider.ID_CMECS, "http://cmecscatalog.org/classification/aquaticSetting/");
            put(TaxonomyProvider.ID_BIO_INFO_REFERENCE, "http://bioinfo.org.uk/html/b");
            put(TaxonomyProvider.ID_PREFIX_GBIF, "http://www.gbif.org/species/");
            put(TaxonomyProvider.ID_PREFIX_INATURALIST, "https://www.inaturalist.org/observations/");
            put(TaxonomyProvider.ATLAS_OF_LIVING_AUSTRALIA.getIdPrefix(), "https://bie.ala.org.au/species/");
            put(TaxonomyProvider.ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY, "http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/afd/taxa/");
            put(TaxonomyProvider.ID_PREFIX_BIODIVERSITY_AUSTRALIA, "http://id.biodiversity.org.au/apni.taxon/");
            put(TaxonomyProvider.ID_PREFIX_INDEX_FUNGORUM, "http://www.indexfungorum.org/names/NamesRecord.asp?RecordID=");
            put(TaxonomyProvider.ID_PREFIX_NCBI, "https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=");
            put(TaxonomyProvider.NCBITaxon.getIdPrefix(), "http://purl.obolibrary.org/obo/NCBITaxon_");
            put(TaxonomyProvider.ID_PREFIX_NBN, "https://data.nbn.org.uk/Taxa/");
            put(TaxonomyProvider.ID_PREFIX_DOI, "https://doi.org/");
            put(TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA.getIdPrefix(), "http://www.marine.csiro.au/mirrorsearch/ir_search.list_species?sp_id=");
            put(TaxonomyProvider.OPEN_TREE_OF_LIFE.getIdPrefix(), "https://tree.opentreeoflife.org/opentree/ottol@");
            put(TaxonomyProvider.ID_PREFIX_HTTP, TaxonomyProvider.ID_PREFIX_HTTP);
            put(TaxonomyProvider.ID_PREFIX_HTTPS, TaxonomyProvider.ID_PREFIX_HTTPS);
            put(TaxonomyProvider.ID_PREFIX_ITIS, "http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=");
            put(TaxonomyProvider.FISHBASE_CACHE.getIdPrefix() + "FB:SpecCode:", "http://fishbase.org/summary/");
            put(TaxonomyProvider.FISHBASE_CACHE.getIdPrefix() + "SLB:SpecCode:", "http://sealifebase.org/Summary/SpeciesSummary.php?id=");
            put(TaxonomyProvider.INATURALIST_TAXON.getIdPrefix(), "https://inaturalist.org/taxa/");
            put(TaxonomyProvider.WIKIDATA.getIdPrefix(), "https://www.wikidata.org/wiki/");
            put(TaxonomyProvider.GEONAMES.getIdPrefix(), "http://www.geonames.org/");
        }};
    }

    public static Map<String, String> getURLSuffixMap() {
        return new HashMap<String, String>() {{
            put(TaxonomyProvider.ID_BIO_INFO_REFERENCE, ".htm");
        }};
    }

    public static boolean isSupported(String externalId) {
        return taxonomyProviderFor(externalId) != null;
    }

    public static TaxonomyProvider taxonomyProviderFor(String externalId) {
        TaxonomyProvider provider = null;
        if (StringUtils.isNotBlank(externalId)) {
            for (TaxonomyProvider prefix : TaxonomyProvider.values()) {
                if (StringUtils.startsWith(externalId, prefix.getIdPrefix())) {
                    provider = prefix;
                    break;
                }
            }
        }
        return provider;
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
        return buildJsonUrl(urlForExternalId(externalId));
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
        return StringUtils.join(nonBlanks, ". ").trim();
    }

    public static String selectValue(Map<String, String> link, String[] candidateIdsInIncreasingPreference) {
        String propertyName = null;
        for (String candidateId : candidateIdsInIncreasingPreference) {
            if (hasProperty(link, candidateId)) {
                propertyName = candidateId;
            }
        }
        return propertyName == null ? "" : link.get(propertyName);
    }

    public static boolean hasProperty(Map<String, String> link, String propertyName) {
        return link.containsKey(propertyName) && org.apache.commons.lang3.StringUtils.isNotBlank(link.get(propertyName));
    }

    public static String stripPrefix(TaxonomyProvider provider, String externalId) {
        return StringUtils.replace(externalId, provider.getIdPrefix(), "");
    }
}
