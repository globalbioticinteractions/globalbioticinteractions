package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.splitPreserveAllTokens;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.eol.globi.domain.PropertyAndValueDictionary.COMMON_NAMES;
import static org.eol.globi.domain.PropertyAndValueDictionary.EXTERNAL_ID;
import static org.eol.globi.domain.PropertyAndValueDictionary.EXTERNAL_URL;
import static org.eol.globi.domain.PropertyAndValueDictionary.NAME;
import static org.eol.globi.domain.PropertyAndValueDictionary.NAME_SOURCE;
import static org.eol.globi.domain.PropertyAndValueDictionary.NAME_SOURCE_ACCESSED_AT;
import static org.eol.globi.domain.PropertyAndValueDictionary.NAME_SOURCE_URL;
import static org.eol.globi.domain.PropertyAndValueDictionary.NO_MATCH;
import static org.eol.globi.domain.PropertyAndValueDictionary.NO_NAME;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH_IDS;
import static org.eol.globi.domain.PropertyAndValueDictionary.PATH_NAMES;
import static org.eol.globi.domain.PropertyAndValueDictionary.RANK;
import static org.eol.globi.domain.PropertyAndValueDictionary.STATUS_ID;
import static org.eol.globi.domain.PropertyAndValueDictionary.STATUS_LABEL;
import static org.eol.globi.domain.PropertyAndValueDictionary.THUMBNAIL_URL;
import static org.eol.globi.domain.TaxonomyProvider.ID_PREFIX_DOI;
import static org.eol.globi.domain.TaxonomyProvider.ID_PREFIX_EOL;
import static org.eol.globi.domain.TaxonomyProvider.PLAZI;
import static org.eol.globi.util.ExternalIdUtil.taxonomyProviderFor;
import static org.eol.globi.util.ExternalIdUtil.urlForExternalId;

public class TaxonUtil {
    public static final String SOURCE_TAXON = "sourceTaxon";
    public static final String SOURCE_TAXON_KINGDOM = SOURCE_TAXON + "Kingdom";
    public static final String SOURCE_TAXON_PHYLUM = SOURCE_TAXON + "Phylum";
    public static final String SOURCE_TAXON_CLASS = SOURCE_TAXON + "Class";
    public static final String SOURCE_TAXON_SUBCLASS = SOURCE_TAXON + "Subclass";
    public static final String SOURCE_TAXON_SUPERORDER = SOURCE_TAXON + "Superorder";
    public static final String SOURCE_TAXON_ORDER = SOURCE_TAXON + "Order";
    public static final String SOURCE_TAXON_SUBORDER = SOURCE_TAXON + "Suborder";
    public static final String SOURCE_TAXON_INFRAORDER = SOURCE_TAXON + "Infraorder";
    public static final String SOURCE_TAXON_PARVORDER = SOURCE_TAXON + "Parvorder";
    public static final String SOURCE_TAXON_SUPERFAMILY = SOURCE_TAXON + "Superfamily";
    public static final String SOURCE_TAXON_FAMILY = SOURCE_TAXON + "Family";
    public static final String SOURCE_TAXON_SUBFAMILY = SOURCE_TAXON + "Subfamily";
    public static final String SOURCE_TAXON_GENUS = SOURCE_TAXON + "Genus";
    public static final String SOURCE_TAXON_SUBGENUS = SOURCE_TAXON + "Subgenus";
    public static final String SOURCE_TAXON_SPECIFIC_EPITHET = SOURCE_TAXON + "SpecificEpithet";
    public static final String SOURCE_TAXON_SUBSPECIFIC_EPITHET = SOURCE_TAXON + "SubspecificEpithet";
    public static final List<String> SOURCE_TAXON_HIGHER_ORDER_RANK_KEYS = Arrays.asList(
            SOURCE_TAXON_SUBGENUS,
            SOURCE_TAXON_GENUS,
            SOURCE_TAXON_SUBFAMILY,
            SOURCE_TAXON_FAMILY,
            SOURCE_TAXON_SUPERFAMILY,
            SOURCE_TAXON_PARVORDER,
            SOURCE_TAXON_INFRAORDER,
            SOURCE_TAXON_SUBORDER,
            SOURCE_TAXON_ORDER,
            SOURCE_TAXON_SUPERORDER,
            SOURCE_TAXON_SUBCLASS,
            SOURCE_TAXON_CLASS,
            SOURCE_TAXON_PHYLUM,
            SOURCE_TAXON_KINGDOM);

    public static final String TARGET_TAXON = "targetTaxon";
    public static final String TARGET_TAXON_KINGDOM = TARGET_TAXON + "Kingdom";
    public static final String TARGET_TAXON_PHYLUM = TARGET_TAXON + "Phylum";
    public static final String TARGET_TAXON_CLASS = TARGET_TAXON + "Class";
    public static final String TARGET_TAXON_SUBCLASS = TARGET_TAXON + "Subclass";
    public static final String TARGET_TAXON_SUPERORDER = TARGET_TAXON + "Superorder";
    public static final String TARGET_TAXON_ORDER = TARGET_TAXON + "Order";
    public static final String TARGET_TAXON_SUBORDER = TARGET_TAXON + "Suborder";
    public static final String TARGET_TAXON_INFRAORDER = TARGET_TAXON + "Infraorder";
    public static final String TARGET_TAXON_PARVORDER = TARGET_TAXON + "Parvorder";
    public static final String TARGET_TAXON_SUPERFAMILY = TARGET_TAXON + "Superfamily";
    public static final String TARGET_TAXON_FAMILY = TARGET_TAXON + "Family";
    public static final String TARGET_TAXON_SUBFAMILY = TARGET_TAXON + "Subfamily";
    public static final String TARGET_TAXON_GENUS = TARGET_TAXON + "Genus";
    public static final String TARGET_TAXON_SUBGENUS = TARGET_TAXON + "Subgenus";
    public static final String TARGET_TAXON_SPECIFIC_EPITHET = TARGET_TAXON + "SpecificEpithet";
    public static final String TARGET_TAXON_SUBSPECIFIC_EPITHET = TARGET_TAXON + "SubspecificEpithet";

    public static final List<String> TARGET_TAXON_HIGHER_ORDER_RANK_KEYS = Arrays.asList(
            TARGET_TAXON_SUBGENUS,
            TARGET_TAXON_GENUS,
            TARGET_TAXON_SUBFAMILY,
            TARGET_TAXON_FAMILY,
            TARGET_TAXON_SUPERFAMILY,
            TARGET_TAXON_PARVORDER,
            TARGET_TAXON_INFRAORDER,
            TARGET_TAXON_SUBORDER,
            TARGET_TAXON_ORDER,
            TARGET_TAXON_SUPERORDER,
            TARGET_TAXON_SUBCLASS,
            TARGET_TAXON_CLASS,
            TARGET_TAXON_PHYLUM,
            TARGET_TAXON_KINGDOM);
    public static final String SOURCE_TAXON_NAME = "sourceTaxonName";
    public static final String SOURCE_TAXON_ID = "sourceTaxonId";
    public static final String SOURCE_TAXON_PATH = "sourceTaxonPath";
    public static final String SOURCE_TAXON_PATH_NAMES = "sourceTaxonPathNames";
    public static final String TARGET_TAXON_PATH = "targetTaxonPath";
    public static final String TARGET_TAXON_PATH_NAMES = "targetTaxonPathNames";
    public static final String TARGET_TAXON_ID = "targetTaxonId";
    public static final String TARGET_TAXON_NAME = "targetTaxonName";

    public static final List<String> TAXON_RANK_PROPERTY_NAMES = new ArrayList<String>() {{
        addAll(SOURCE_TAXON_HIGHER_ORDER_RANK_KEYS);
        add(SOURCE_TAXON_SPECIFIC_EPITHET);
        add(SOURCE_TAXON_SUBSPECIFIC_EPITHET);
        addAll(TARGET_TAXON_HIGHER_ORDER_RANK_KEYS);
        add(TARGET_TAXON_SPECIFIC_EPITHET);
        add(TARGET_TAXON_SUBSPECIFIC_EPITHET);
    }};
    public static final String SOURCE_TAXON_RANK = SOURCE_TAXON + "Rank";
    public static final String SOURCE_TAXON_PATH_IDS = SOURCE_TAXON_PATH + "Ids";
    public static final String TARGET_TAXON_PATH_IDS = TARGET_TAXON_PATH + "Ids";
    public static final String TARGET_TAXON_RANK = TARGET_TAXON + "Rank";

    public static final String TARGET_TAXON_SPECIES = TARGET_TAXON + "Species";
    public static final String SOURCE_TAXON_SPECIES = SOURCE_TAXON + "Species";
    public static final List<String> TAXON_RANK_NAMES = Arrays.asList("kingdom", "phylum", "class", "order", "family", "genus");

    public static Map<String, String> taxonToMap(Taxon taxon) {
        return taxonToMap(taxon, "");
    }

    public static Map<String, String> taxonToMap(Taxon taxon, String prefix) {
        Map<String, String> properties = new HashMap<>();
        properties.put(prefix + NAME, taxon.getName());
        properties.put(prefix + RANK, taxon.getRank());
        properties.put(prefix + EXTERNAL_ID, taxon.getExternalId());
        properties.put(prefix + PATH, taxon.getPath());
        properties.put(prefix + PATH_IDS, taxon.getPathIds());
        properties.put(prefix + PATH_NAMES, taxon.getPathNames());
        properties.put(prefix + COMMON_NAMES, taxon.getCommonNames());
        if (isBlank(taxon.getExternalUrl()) && isNotBlank(taxon.getExternalId())) {
            properties.put(prefix + EXTERNAL_URL, urlForExternalId(taxon.getExternalId()));
        } else {
            properties.put(prefix + EXTERNAL_URL, taxon.getExternalUrl());
        }

        properties.put(prefix + THUMBNAIL_URL, taxon.getThumbnailUrl());
        Term status = taxon.getStatus();
        if (status != null
                && isNotBlank(status.getId())
                && isNotBlank(status.getName())) {
            properties.put(prefix + STATUS_ID, status.getId());
            properties.put(prefix + STATUS_LABEL, status.getName());
        }

        properties.put(prefix + NAME_SOURCE, taxon.getNameSource());
        properties.put(prefix + NAME_SOURCE_URL, taxon.getNameSourceURL());
        properties.put(prefix + NAME_SOURCE_ACCESSED_AT, taxon.getNameSourceAccessedAt());

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
        if (isBlank(externalUrl) && isNotBlank(externalId)) {
            taxon.setExternalUrl(urlForExternalId(externalId));
        } else {
            taxon.setExternalUrl(externalUrl);
        }

        taxon.setThumbnailUrl(properties.get(THUMBNAIL_URL));

        String statusId = properties.get(STATUS_ID);
        String statusLabel = properties.get(STATUS_LABEL);
        if (isNotBlank(statusId) && isNotBlank(statusLabel)) {
            taxon.setStatus(new TermImpl(statusId, statusLabel));
        }

        taxon.setNameSource(properties.get(NAME_SOURCE));
        taxon.setNameSourceURL(properties.get(NAME_SOURCE_URL));
        taxon.setNameSourceAccessedAt(properties.get(NAME_SOURCE_ACCESSED_AT));
    }

    public static Taxon enrich(PropertyEnricher enricher, Taxon taxon) throws PropertyEnricherException {
        Map<String, String> properties = taxonToMap(taxon);
        Taxon enrichedTaxon = new TaxonImpl();
        mapToTaxon(enricher.enrichFirstMatch(properties), enrichedTaxon);
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
            Map<String, String> pathMapA = toPathNameMap(taxonA);
            Map<String, String> pathMapB = toPathNameMap(taxonB);
            return hasHigherOrderTaxaMismatch(pathMapA, pathMapB)
                    || taxonPathLengthMismatch(pathMapA, pathMapB);
        } else {
            return false;
        }
    }

    public static boolean overlap(Taxon taxonA, Taxon taxonB) {
        if (isResolved(taxonA) && isResolved(taxonB)) {
            String[] pathA = split(taxonA.getPath(), CharsetConstant.SEPARATOR_CHAR);
            String[] pathB = split(taxonB.getPath(), CharsetConstant.SEPARATOR_CHAR);
            final Set<String> setA = Arrays.stream(pathA).map(StringUtils::trim).collect(Collectors.toCollection(HashSet::new));
            final Set<String> setB = Arrays.stream(pathB).map(StringUtils::trim).collect(Collectors.toCollection(HashSet::new));
            return setA.containsAll(setB) || setB.containsAll(setA);
        } else {
            return false;
        }
    }

    public static List<Taxon> determineNonOverlappingTaxa(List<Taxon> collectTaxa) {
        List<Taxon> nonOverlapping = new ArrayList<>(collectTaxa);
        List<Taxon> overlapping;
        while ((overlapping = nextOverlapping(nonOverlapping)).size() == 2) {
            final Taxon first = overlapping.get(0);
            final String[] split1 = split(first.getPath(), CharsetConstant.SEPARATOR_CHAR);
            final Taxon second = overlapping.get(1);
            final String[] split2 = split(second.getPath(), CharsetConstant.SEPARATOR_CHAR);
            if (split1 != null && split2 != null && split1.length > split2.length) {
                nonOverlapping.remove(first);
            } else {
                nonOverlapping.remove(second);
            }
        }
        return nonOverlapping;
    }

    public static List<Taxon> nextOverlapping(List<Taxon> collectTaxa) {
        for (Taxon taxon : collectTaxa) {
            for (Taxon taxon1 : collectTaxa) {
                if (taxon != taxon1) {
                    if (TaxonUtil.overlap(taxon, taxon1)) {
                        return Arrays.asList(taxon, taxon1);
                    }
                }
            }
        }
        return Collections.emptyList();
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

    private static boolean higherOrderTaxaMatch(Map<String, String> pathMapA, Map<String, String> pathMapB) {
        boolean allMatch = true;
        boolean hasAtLeastOneSharedRank = false;
        String[] ranks = new String[]{"phylum", "class", "order", "family"};
        for (String rank : ranks) {
            if (pathMapA.containsKey(rank) && pathMapB.containsKey(rank)) {
                final String rankValueA = pathMapA.get(rank);
                final String rankValueB = pathMapB.get(rank);
                hasAtLeastOneSharedRank = true;
                allMatch = allMatch && StringUtils.equals(rankValueA, rankValueB);
            }
        }
        return hasAtLeastOneSharedRank && allMatch;
    }

    public static boolean taxonPathLengthMismatch(Map<String, String> pathMapA, Map<String, String> pathMapB) {
        return Math.min(pathMapA.size(), pathMapB.size()) == 1 && Math.max(pathMapA.size(), pathMapB.size()) > 4;
    }

    public static Map<String, String> toPathNameMap(Taxon taxonA) {
        return toPathNameMap(taxonA, taxonA.getPath());
    }

    public static Map<String, String> toPathIdMap(Taxon taxonA) {
        return toPathNameMap(taxonA, taxonA.getPathIds());
    }

    private static Map<String, String> toPathNameMap(Taxon taxonA, String pathElements) {
        String[] pathParts = splitPreserveAllTokens(pathElements, CharsetConstant.SEPARATOR_CHAR);
        String[] pathNames = splitPreserveAllTokens(taxonA.getPathNames(), CharsetConstant.SEPARATOR_CHAR);
        Map<String, String> pathMap = new HashMap<>();
        if (pathParts != null && pathNames != null && pathParts.length == pathNames.length) {
            for (int i = 0; i < pathParts.length; i++) {
                pathMap.put(trim(lowerCase(pathNames[i])), trim(pathParts[i]));
            }
        }
        return pathMap;
    }

    public static TaxonImage enrichTaxonImageWithTaxon(Map<String, String> taxon, TaxonImage taxonImage) {
        return enrichTaxonImageWithTaxon(taxon, taxonImage, "en");
    }

    public static TaxonImage enrichTaxonImageWithTaxon(
            Map<String, String> taxon,
            TaxonImage taxonImage,
            String preferredLanguage) {
        return (taxonImage == null || taxon == null)
                ? null
                : enrich(taxon, taxonImage, preferredLanguage);
    }

    private static TaxonImage enrich(Map<String, String> taxon, TaxonImage taxonImage, String preferredLanguage) {
        String commonName = isBlank(taxonImage.getCommonName())
                ? taxon.get(COMMON_NAMES)
                : taxonImage.getCommonName();

        eraseLanguageTag(taxonImage, preferredLanguage, commonName);

        if (isBlank(taxonImage.getScientificName())) {
            taxonImage.setScientificName(taxon.get(NAME));
        }
        if (isBlank(taxonImage.getTaxonPath())) {
            taxonImage.setTaxonPath(taxon.get(PATH));
        }
        if (isBlank(taxonImage.getInfoURL())) {
            taxonImage.setInfoURL(taxon.get(EXTERNAL_URL));
        }
        if (isBlank(taxonImage.getThumbnailURL())) {
            String thumbnailURL = taxon.get(THUMBNAIL_URL);
            if (!contains(thumbnailURL, "media.eol.org")) {
                taxonImage.setThumbnailURL(thumbnailURL);
            }
        }

        if (isNotBlank(taxonImage.getThumbnailURL())) {
            String thumbnailURL = taxonImage.getThumbnailURL();
            taxonImage.setThumbnailURL(replace(thumbnailURL, "http://media.eol.org", "https://media.eol.org"));
        }

        if (isBlank(taxonImage.getPageId())) {
            String externalId = taxon.get(EXTERNAL_ID);
            if (startsWith(externalId, ID_PREFIX_EOL)) {
                taxonImage.setPageId(externalId.replace(ID_PREFIX_EOL, ""));
            }
        }
        return taxonImage;
    }

    private static void eraseLanguageTag(TaxonImage taxonImage, String preferredLanguage, String commonName) {
        if (isNotBlank(commonName)) {
            String[] splits = split(commonName, CharsetConstant.SEPARATOR_CHAR);
            for (String split : splits) {
                if (contains(split, "@" + preferredLanguage)) {
                    taxonImage.setCommonName(trim(replace(split, "@" + preferredLanguage, "")));
                }
            }
        }
    }

    public static boolean isResolved(Taxon taxon) {
        return taxon != null
                && isNotBlank(taxon.getPath())
                && isNotBlank(taxon.getName())
                && isNotBlank(taxon.getExternalId());
    }

    public static boolean isResolved(Map<String, String> properties) {
        return properties != null
                && isNotBlank(properties.get(NAME))
                && isNotBlank(properties.get(EXTERNAL_ID))
                && isNotBlank(properties.get(PATH));
    }

    public static boolean isNonEmptyValue(String value) {
        return isNotBlank(value)
                && !StringUtils.equals(value, NO_MATCH)
                && !StringUtils.equals(value, NO_NAME);
    }

    public static boolean isEmptyValue(String value) {
        return !isNonEmptyValue(value);
    }

    public static String generateTargetTaxonPath(Map<String, String> properties) {
        return generateTaxonPath(properties,
                getAllTargetTaxonRanks(),
                TARGET_TAXON_GENUS,
                TARGET_TAXON_SPECIFIC_EPITHET,
                TARGET_TAXON_SUBSPECIFIC_EPITHET,
                TARGET_TAXON_SPECIES);
    }

    public static String generateTaxonPath(Map<String, String> properties,
                                           List<String> allRanks,
                                           String genusRank,
                                           String specificEpithetRank,
                                           String subspecificEpithetRank) {
        return generateTaxonPath(properties, allRanks, genusRank, specificEpithetRank, subspecificEpithetRank, null);
    }

    public static String generateTaxonPath(Map<String, String> properties,
                                           List<String> allRanks,
                                           String genusRank,
                                           String specificEpithetRank,
                                           String subspecificEpithetRank,
                                           String speciesRank) {
        Stream<String> rankValues = allRanks
                .stream()
                .map(properties::get)
                .filter(StringUtils::isNotBlank);

        String species = trim(generateSpeciesName(properties, genusRank, specificEpithetRank, subspecificEpithetRank, speciesRank));

        Stream<String> ranksWithSpecies = isBlank(species) ? rankValues : Stream.concat(rankValues, Stream.of(species));
        return ranksWithSpecies
                .collect(Collectors.joining(CharsetConstant.SEPARATOR));
    }

    public static String generateTaxonPathNames(Map<String, String> properties,
                                                List<String> allRanks,
                                                String keyPrefix,
                                                String genusRank,
                                                String specificEpithetRank,
                                                String subspecificEpithetRank,
                                                String speciesRank) {
        Stream<String> rankLabels = allRanks
                .stream()
                .map(x -> Pair.of(x, properties.get(x)))
                .filter(x -> isNotBlank(x.getValue()))
                .map(x -> lowerCase(replace(x.getKey(), keyPrefix, "")));

        String species = trim(generateSpeciesName(properties, genusRank, specificEpithetRank, subspecificEpithetRank, speciesRank));
        Stream<String> ranksWithSpecies = isBlank(species)
                ? rankLabels
                : Stream.concat(rankLabels, Stream.of("species"));

        return ranksWithSpecies
                .collect(Collectors.joining(CharsetConstant.SEPARATOR));
    }

    public final static List<String> getAllTargetTaxonRanks() {
        ArrayList<String> allRanks = new ArrayList<>(TARGET_TAXON_HIGHER_ORDER_RANK_KEYS);
        Collections.reverse(allRanks);
        return allRanks;
    }

    public final static List<String> getAllSourceTaxonRanks() {
        ArrayList<String> allRanks = new ArrayList<>(SOURCE_TAXON_HIGHER_ORDER_RANK_KEYS);
        Collections.reverse(allRanks);
        return allRanks;
    }

    public static String generateTargetTaxonPathNames(Map<String, String> properties) {
        return generateTaxonPathNames(properties,
                getAllTargetTaxonRanks(),
                "targetTaxon",
                TARGET_TAXON_GENUS,
                TARGET_TAXON_SPECIFIC_EPITHET,
                TARGET_TAXON_SUBSPECIFIC_EPITHET,
                TARGET_TAXON_SPECIES);
    }

    public static String generateSourceTaxonPath(Map<String, String> properties) {
        return generateTaxonPath(properties,
                getAllSourceTaxonRanks(),
                SOURCE_TAXON_GENUS,
                SOURCE_TAXON_SPECIFIC_EPITHET,
                SOURCE_TAXON_SUBSPECIFIC_EPITHET,
                SOURCE_TAXON_SPECIES);
    }

    public static String generateSourceTaxonPathNames(Map<String, String> properties) {
        return generateTaxonPathNames(properties, getAllSourceTaxonRanks(),
                "sourceTaxon",
                SOURCE_TAXON_GENUS,
                SOURCE_TAXON_SPECIFIC_EPITHET,
                SOURCE_TAXON_SUBSPECIFIC_EPITHET,
                SOURCE_TAXON_SPECIES);
    }


    public static String generateTaxonName(Map<String, String> properties,
                                           List<String> higherOrderRankKeys,
                                           String genusKey,
                                           String specificEpithetKey,
                                           String subspecificEpithetKey,
                                           String speciesKey) {

        String taxonName = generateSpeciesName(properties, genusKey, specificEpithetKey, subspecificEpithetKey, speciesKey);

        if (isBlank(taxonName)) {
            for (String rankName : higherOrderRankKeys) {
                final String name = properties.get(rankName);
                if (isNotBlank(name)) {
                    taxonName = name;
                    break;
                }
            }
        }
        return taxonName;
    }

    public static String generateSpeciesName(Map<String, String> properties, String genusKey, String specificEpithetKey, String subspecificEpithetKey, String speciesKey) {
        String speciesName = null;
        if (isNotBlank(genusKey)
                && properties.containsKey(genusKey)
                && isNotBlank(specificEpithetKey)
                && properties.containsKey(specificEpithetKey)) {
            List<String> speciesNameParts = Arrays.asList(
                    properties.get(genusKey),
                    properties.get(specificEpithetKey),
                    properties.get(subspecificEpithetKey));
            speciesName = trim(join(speciesNameParts, " "));
        } else if (isNotBlank(speciesKey)
                && properties.containsKey(speciesKey)) {
            speciesName = trim(properties.get(speciesKey));
        }
        return speciesName;
    }

    public static String generateSourceTaxonName(Map<String, String> properties) {
        return generateTaxonName(properties,
                SOURCE_TAXON_HIGHER_ORDER_RANK_KEYS,
                SOURCE_TAXON_GENUS,
                SOURCE_TAXON_SPECIFIC_EPITHET,
                SOURCE_TAXON_SUBSPECIFIC_EPITHET,
                SOURCE_TAXON_SPECIES
        );
    }

    public static String generateTargetTaxonName(Map<String, String> properties) {
        return generateTaxonName(properties,
                TARGET_TAXON_HIGHER_ORDER_RANK_KEYS,
                TARGET_TAXON_GENUS,
                TARGET_TAXON_SPECIFIC_EPITHET,
                TARGET_TAXON_SUBSPECIFIC_EPITHET,
                TARGET_TAXON_SPECIES
        );
    }

    public static void enrichTaxonNames(Map<String, String> properties) {
        if (!properties.containsKey(SOURCE_TAXON_NAME)) {
            properties.put(SOURCE_TAXON_NAME, generateSourceTaxonName(properties));
        }

        if (!properties.containsKey(SOURCE_TAXON_PATH)) {
            String path = generateSourceTaxonPath(properties);
            if (isNotBlank(path)) {
                properties.put(SOURCE_TAXON_PATH, path);
                properties.put(SOURCE_TAXON_PATH_NAMES, generateSourceTaxonPathNames(properties));
            }
        }

        if (!properties.containsKey(TARGET_TAXON_NAME)) {
            properties.put(TARGET_TAXON_NAME, generateTargetTaxonName(properties));
        }

        if (!properties.containsKey(TARGET_TAXON_PATH)) {
            String path = generateTargetTaxonPath(properties);
            if (isNotBlank(path)) {
                properties.put(TARGET_TAXON_PATH, path);
                properties.put(TARGET_TAXON_PATH_NAMES, generateTargetTaxonPathNames(properties));
            }
        }
    }

    public static boolean nonBlankNodeOrNonBlankId(Taxon taxon) {
        return taxon != null
                && (isNotBlank(taxon.getName()) || isNotBlank(taxon.getId()));
    }

    public static String generateTaxonPath(Map<String, String> nameMap) {
        return generateTaxonPath(nameMap, TAXON_RANK_NAMES, "genus", "specificEpithet", "subspecificEpithet");
    }

    public static String generateTaxonPathNames(Map<String, String> nameMap) {
        return generateTaxonPathNames(nameMap, TAXON_RANK_NAMES, "", "genus", "specificEpithet", "subspecificEpithet", "species");
    }

    public static boolean hasLiteratureReference(Taxon taxon) {
        return (taxon != null
                &&
                (startsWith(taxon.getExternalId(), ID_PREFIX_DOI)
                        || PLAZI.equals(taxonomyProviderFor(taxon.getExternalId()))));
    }
}
