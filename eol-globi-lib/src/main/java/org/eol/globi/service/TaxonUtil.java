package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.Version;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.util.DateUtil;
import org.eol.globi.util.ExternalIdUtil;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.eol.globi.domain.PropertyAndValueDictionary.*;
import static org.eol.globi.domain.PropertyAndValueDictionary.EXTERNAL_URL;

public class TaxonUtil {
    public static Map<String, String> taxonToMap(Taxon taxon) {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(NAME, taxon.getName());
        properties.put(RANK, taxon.getRank());
        properties.put(EXTERNAL_ID, taxon.getExternalId());
        properties.put(PATH, taxon.getPath());
        properties.put(PATH_IDS, taxon.getPathIds());
        properties.put(PATH_NAMES, taxon.getPathNames());
        properties.put(COMMON_NAMES, taxon.getCommonNames());
        if (StringUtils.isBlank(taxon.getExternalUrl()) && StringUtils.isNotBlank(taxon.getExternalId())) {
            properties.put(EXTERNAL_URL, ExternalIdUtil.urlForExternalId(taxon.getExternalId()));
        } else {
            properties.put(EXTERNAL_URL, taxon.getExternalUrl());
        }

        properties.put(THUMBNAIL_URL, taxon.getThumbnailUrl());
        Term status = taxon.getStatus();
        if (status != null
                && StringUtils.isNotBlank(status.getId())
                && StringUtils.isNotBlank(status.getName())) {
            properties.put(STATUS_ID, status.getId());
            properties.put(STATUS_LABEL, status.getName());
        }

        properties.put(NAME_SOURCE, taxon.getNameSource());
        properties.put(NAME_SOURCE_URL, taxon.getNameSourceURL());
        properties.put(NAME_SOURCE_ACCESSED_AT, taxon.getNameSourceAccessedAt());

        return Collections.unmodifiableMap(properties);
    }

    public static void mapToTaxon(Map<String, String> properties, Taxon taxon) {
        taxon.setName(properties.get(NAME));
        taxon.setRank(properties.get(RANK));
        final String externalId = properties.get(EXTERNAL_ID);
        taxon.setExternalId(externalId);
        taxon.setPath(properties.get(PATH));
        taxon.setPathIds(properties.get(PATH_IDS));
        taxon.setPathNames(properties.get(PATH_NAMES));
        taxon.setCommonNames(properties.get(COMMON_NAMES));

        final String externalUrl = properties.get(EXTERNAL_URL);
        if (StringUtils.isBlank(externalUrl) && StringUtils.isNotBlank(externalId)) {
            taxon.setExternalUrl(ExternalIdUtil.urlForExternalId(externalId));
        } else {
            taxon.setExternalUrl(externalUrl);
        }

        taxon.setThumbnailUrl(properties.get(THUMBNAIL_URL));

        String statusId = properties.get(STATUS_ID);
        String statusLabel = properties.get(STATUS_LABEL);
        if (StringUtils.isNotBlank(statusId) && StringUtils.isNotBlank(statusLabel)) {
            taxon.setStatus(new TermImpl(statusId, statusLabel));
        }

        taxon.setNameSource(properties.get(NAME_SOURCE));
        taxon.setNameSourceURL(properties.get(NAME_SOURCE_URL));
        taxon.setNameSourceAccessedAt(properties.get(NAME_SOURCE_ACCESSED_AT));
    }

    public static Taxon enrich(PropertyEnricher enricher, Taxon taxon) throws PropertyEnricherException {
        Map<String, String> properties = taxonToMap(taxon);
        Taxon enrichedTaxon = new TaxonImpl();
        mapToTaxon(enricher.enrich(properties), enrichedTaxon);
        return enrichedTaxon;
    }

    public static Taxon mapToTaxon(Map<String, String> properties) {
        Taxon taxon = new TaxonImpl();
        mapToTaxon(properties, taxon);
        return taxon;
    }

    public static Taxon copy(Taxon taxon) {
        TaxonImpl taxonCopy = new TaxonImpl();
        mapToTaxon(taxonToMap(taxon), taxonCopy);
        return taxonCopy;
    }

    public static Taxon copy(Taxon srcTaxon, Taxon targetTaxon) {
        mapToTaxon(taxonToMap(srcTaxon), targetTaxon);
        return targetTaxon;
    }

    public static boolean likelyHomonym(Taxon taxonA, Taxon taxonB) {
        if (isResolved(taxonA) && isResolved(taxonB)) {
            Map<String, String> pathMapA = toPathMap(taxonA);
            Map<String, String> pathMapB = toPathMap(taxonB);
            return hasHigherOrderTaxaMismatch(pathMapA, pathMapB)
                    || taxonPathLengthMismatch(pathMapA, pathMapB);
        } else {
            return false;
        }
    }

    private static boolean hasHigherOrderTaxaMismatch(Map<String, String> pathMapA, Map<String, String> pathMapB) {
        boolean hasAtLeastOneMatchingRank = false;
        boolean hasAtLeastOneSharedRank = false;
        String[] ranks = new String[]{"phylum", "class", "order", "family"};
        for (String rank : ranks) {
            if (pathMapA.containsKey(rank) && pathMapB.containsKey(rank)) {
                final String rankValueA = pathMapA.get(rank);
                final String rankValueB = pathMapB.get(rank);
                hasAtLeastOneSharedRank = true;
                if (StringUtils.equals(rankValueA, rankValueB)) {
                    hasAtLeastOneMatchingRank = true;
                    break;
                }
            }
        }
        return hasAtLeastOneSharedRank && !hasAtLeastOneMatchingRank;
    }

    public static boolean taxonPathLengthMismatch(Map<String, String> pathMapA, Map<String, String> pathMapB) {
        return Math.min(pathMapA.size(), pathMapB.size()) == 1 && Math.max(pathMapA.size(), pathMapB.size()) > 4;
    }

    private static Map<String, String> toPathMap(Taxon taxonA) {
        String[] pathNames = StringUtils.splitPreserveAllTokens(taxonA.getPathNames(), CharsetConstant.SEPARATOR_CHAR);
        String[] path = StringUtils.splitPreserveAllTokens(taxonA.getPath(), CharsetConstant.SEPARATOR_CHAR);
        Map<String, String> pathMap = new HashMap<String, String>();
        if (pathNames != null && path != null && pathNames.length == path.length) {
            for (int i = 0; i < pathNames.length; i++) {
                pathMap.put(StringUtils.trim(StringUtils.lowerCase(pathNames[i])), StringUtils.trim(path[i]));
            }
        }
        return pathMap;
    }

    public static TaxonImage enrichTaxonImageWithTaxon(Map<String, String> taxon, TaxonImage taxonImage) {
        if (StringUtils.isBlank(taxonImage.getCommonName())) {
            String commonName = taxon.get(COMMON_NAMES);
            if (StringUtils.isNotBlank(commonName)) {
                String[] splits = StringUtils.split(commonName, CharsetConstant.SEPARATOR_CHAR);
                for (String split : splits) {
                    if (StringUtils.contains(split, "@en")) {
                        taxonImage.setCommonName(StringUtils.trim(StringUtils.replace(split, "@en", "")));
                    }
                }
            }
        }

        if (StringUtils.isBlank(taxonImage.getScientificName())) {
            taxonImage.setScientificName(taxon.get(NAME));
        }
        if (StringUtils.isBlank(taxonImage.getTaxonPath())) {
            taxonImage.setTaxonPath(taxon.get(PATH));
        }
        if (StringUtils.isBlank(taxonImage.getInfoURL())) {
            taxonImage.setInfoURL(taxon.get(EXTERNAL_URL));
        }
        if (StringUtils.isBlank(taxonImage.getThumbnailURL())) {
            taxonImage.setThumbnailURL(taxon.get(THUMBNAIL_URL));
        }

        if (StringUtils.isNotBlank(taxonImage.getThumbnailURL())) {
            String thumbnailURL = taxonImage.getThumbnailURL();
            taxonImage.setThumbnailURL(StringUtils.replace(thumbnailURL, "http://media.eol.org", "https://media.eol.org"));
        }

        if (StringUtils.isBlank(taxonImage.getPageId())) {
            String externalId = taxon.get(EXTERNAL_ID);
            if (StringUtils.startsWith(externalId, TaxonomyProvider.ID_PREFIX_EOL)) {
                taxonImage.setPageId(externalId.replace(TaxonomyProvider.ID_PREFIX_EOL, ""));
            }
        }
        return taxonImage;
    }

    public static boolean isResolved(Taxon taxon) {
        return taxon != null
                && StringUtils.isNotBlank(taxon.getPath())
                && StringUtils.isNotBlank(taxon.getName())
                && StringUtils.isNotBlank(taxon.getExternalId());
    }

    public static boolean isResolved(Map<String, String> properties) {
        return properties != null
                && StringUtils.isNotBlank(properties.get(NAME))
                && StringUtils.isNotBlank(properties.get(EXTERNAL_ID))
                && StringUtils.isNotBlank(properties.get(PATH));
    }

    public static boolean isNonEmptyValue(String value) {
        return StringUtils.isNotBlank(value)
                && !StringUtils.equals(value, NO_MATCH)
                && !StringUtils.equals(value, NO_NAME);
    }

    public static boolean isEmptyValue(String value) {
        return !isNonEmptyValue(value);
    }

    public static Map<String, String> appendNameSourceInfo(Map<String, String> enrichedProperties, final Class serviceClass, final Date date) {
        enrichedProperties = new TreeMap<String, String>(enrichedProperties) {
            {
                put(NAME_SOURCE, serviceClass.getSimpleName());
                put(NAME_SOURCE_URL, Version.getGitHubBaseUrl() + "/eol-globi-taxon-resolver/src/main/java/" + serviceClass.getName().replace(".", "/") + ".java");
                put(NAME_SOURCE_ACCESSED_AT, DateUtil.printDate(date));
            }
        };
        return enrichedProperties;
    }
}
