package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.TaxonomyProvider;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ExternalIdUtil {

    private static final Map<String, String> PREFIX_MAP = new HashMap<String, String>() {{
        put(TaxonomyProvider.ID_PREFIX_EOL, "http://eol.org/pages/");
        put(TaxonomyProvider.EOL_V2.getIdPrefix(), "https://doi.org/10.5281/zenodo.1495266#");
        put(TaxonomyProvider.ID_PREFIX_WORMS, "https://www.marinespecies.org/aphia.php?p=taxdetails&id=");
        put(TaxonomyProvider.ID_PREFIX_ENVO, "http://purl.obolibrary.org/obo/ENVO_");
        put(TaxonomyProvider.ID_PREFIX_WIKIPEDIA, "http://wikipedia.org/wiki/");
        put(TaxonomyProvider.ID_PREFIX_GULFBASE, "http://gulfbase.org/biogomx/biospecies.php?species=");
        put(TaxonomyProvider.ID_PREFIX_GAME, "https://public.myfwc.com/FWRI/GAME/Survey.aspx?id=");
        put(TaxonomyProvider.ID_CMECS, "https://cmecscatalog.org/cmecs/classification/aquaticSetting/");
        put(TaxonomyProvider.ID_BIO_INFO_REFERENCE, "http://bioinfo.org.uk/html/b");
        put(TaxonomyProvider.ID_PREFIX_GBIF, "http://www.gbif.org/species/");
        put(TaxonomyProvider.ID_PREFIX_INATURALIST, "https://www.inaturalist.org/observations/");
        put(TaxonomyProvider.ATLAS_OF_LIVING_AUSTRALIA.getIdPrefix(), "https://bie.ala.org.au/species/");
        put(TaxonomyProvider.ID_PREFIX_AUSTRALIAN_FAUNAL_DIRECTORY, "http://www.environment.gov.au/biodiversity/abrs/online-resources/fauna/afd/taxa/");
        put(TaxonomyProvider.ID_PREFIX_BIODIVERSITY_AUSTRALIA, "http://id.biodiversity.org.au/apni.taxon/");
        put(TaxonomyProvider.ID_PREFIX_INDEX_FUNGORUM, "http://www.indexfungorum.org/names/NamesRecord.asp?RecordID=");
        put(TaxonomyProvider.ID_PREFIX_NCBI, "https://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=");
        put(TaxonomyProvider.ID_PREFIX_NBN, "https://data.nbn.org.uk/Taxa/");
        put("doi:", "https://doi.org/");
        put("DOI:", "https://doi.org/");
        put(TaxonomyProvider.INTERIM_REGISTER_OF_MARINE_AND_NONMARINE_GENERA.getIdPrefix(), "https://www.irmng.org/aphia.php?p=taxdetails&id=");
        put(TaxonomyProvider.OPEN_TREE_OF_LIFE.getIdPrefix(), "https://tree.opentreeoflife.org/opentree/ottol@");
        put(TaxonomyProvider.ID_PREFIX_HTTP, TaxonomyProvider.ID_PREFIX_HTTP);
        put(TaxonomyProvider.ID_PREFIX_HTTPS, TaxonomyProvider.ID_PREFIX_HTTPS);
        put(TaxonomyProvider.ID_PREFIX_ITIS, "http://www.itis.gov/servlet/SingleRpt/SingleRpt?search_topic=TSN&search_value=");
        put(TaxonomyProvider.FISHBASE_SPECCODE.getIdPrefix(), "http://fishbase.org/summary/");
        put(TaxonomyProvider.SEALIFEBASE_SPECCODE.getIdPrefix(), "http://sealifebase.org/Summary/SpeciesSummary.php?id=");
        put(TaxonomyProvider.INATURALIST_TAXON.getIdPrefix(), "https://inaturalist.org/taxa/");
        put(TaxonomyProvider.WIKIDATA.getIdPrefix(), "https://www.wikidata.org/wiki/");
        put(TaxonomyProvider.GEONAMES.getIdPrefix(), "http://www.geonames.org/");
        put(TaxonomyProvider.MSW.getIdPrefix(), "http://www.departments.bucknell.edu/biology/resources/msw3/browse.asp?s=y&id=");
        put(TaxonomyProvider.PLAZI.getIdPrefix(), "http://treatment.plazi.org/id/");
        put(TaxonomyProvider.PLAZI_TAXON_CONCEPT.getIdPrefix(), "http://taxon-concept.plazi.org/id/");
        put(TaxonomyProvider.OPEN_BIODIV.getIdPrefix(), "http://openbiodiv.net/");
        put(TaxonomyProvider.BOLD_BIN.getIdPrefix(), "http://bins.boldsystems.org/index.php/Public_BarcodeCluster?clusteruri=BOLD:");
        put(TaxonomyProvider.BOLD_TAXON.getIdPrefix(), "http://www.boldsystems.org/index.php/Taxbrowser_Taxonpage?taxid=");
        put(TaxonomyProvider.BATBASE_INTERACTION.getIdPrefix(), "https://batbase.org/interaction/");
        put(TaxonomyProvider.BATBASE.getIdPrefix(), "https://batbase.org/taxon/");
        put(TaxonomyProvider.CATALOGUE_OF_LIFE.getIdPrefix(), "https://www.catalogueoflife.org/data/taxon/");
    }};

    private static final Map<String, String> URL_TO_PREFIX_MAP = new HashMap<String, String>() {{
        for (Entry<String, String> prefixUrl : PREFIX_MAP.entrySet()) {
            put(prefixUrl.getValue(), prefixUrl.getKey());
        }

    }};

    private static final Logger LOG = LoggerFactory.getLogger(ExternalIdUtil.class);

    private static final Pattern LIKELY_ID_PATTERN = Pattern.compile(".*[:-].*");

    public static String urlForExternalId(String externalId) {
        URI uri = null;
        String url = null;
        if (externalId != null) {
            for (Map.Entry<String, String> idPrefixToUrlPrefix : getURLPrefixMap().entrySet()) {
                String idPrefix = idPrefixToUrlPrefix.getKey();
                if (StringUtils.startsWith(externalId, idPrefix)) {
                    if (DOI.isCommonlyUsedDoiPrefix(idPrefix)) {
                        try {
                            DOI doi = DOI.create(externalId);
                            url = doi.toURI().toString();
                        } catch (MalformedDOIException e) {
                            LOG.warn("found malformed doi [" + externalId + "]", e);
                        }
                    } else {
                        url = idPrefixToUrlPrefix.getValue() + externalId.replaceAll(idPrefix, "");
                    }
                    String suffix = getURLSuffixMap().get(idPrefix);
                    if (StringUtils.isNotBlank(suffix)) {
                        url = url + suffix;
                    }
                }
                if (url != null) {
                    try {
                        uri = new URI(url);
                    } catch (URISyntaxException e) {
                        //
                    }

                    break;
                }
            }
        }
        return uri == null ? null : uri.toString();
    }

    public static Map<String, String> getURLPrefixMap() {
        return PREFIX_MAP;
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
                for (String idPrefix : prefix.getIdPrefixes()) {
                    if (StringUtils.startsWith(externalId, idPrefix)) {
                        provider = prefix;
                        break;
                    }
                }
            }
        }
        return provider;
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
        String strippedShortest = externalId;
        for (String idPrefix : provider.getIdPrefixes()) {
            String stripped = StringUtils.replace(externalId, idPrefix, "");
            if (StringUtils.length(stripped) < StringUtils.length(strippedShortest)) {
                strippedShortest = stripped;
            }
        }
        return StringUtils.trim(strippedShortest);
    }

    public static String prefixForUrl(String url) {
        return URL_TO_PREFIX_MAP.get(url);
    }

    public static boolean isLikelyId(String idCandidate) {
        return !StringUtils.isBlank(idCandidate) && LIKELY_ID_PATTERN.matcher(idCandidate).matches();
    }

    public static boolean isUnlikelyId(String idCandidate) {
        return !isLikelyId(idCandidate);
    }
}
