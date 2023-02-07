package org.eol.globi.service;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImage;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;
import org.eol.globi.util.InteractUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import static org.eol.globi.domain.PropertyAndValueDictionary.AUTHORSHIP;
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
    public static final String KINGDOM = "Kingdom";
    public static final String PHYLUM = "Phylum";
    public static final String CLASS = "Class";
    public static final String SUBCLASS = "Subclass";
    public static final String SUPERORDER = "Superorder";
    public static final String ORDER = "Order";
    public static final String SUBORDER = "Suborder";
    public static final String INFRAORDER = "Infraorder";
    public static final String PARVORDER = "Parvorder";
    public static final String SUPERFAMILY = "Superfamily";
    public static final String FAMILY = "Family";
    public static final String SUBFAMILY = "Subfamily";
    public static final String GENUS = "Genus";
    public static final String SUBGENUS = "Subgenus";
    public static final String SPECIES = "Species";
    public static final String SPECIFIC_EPITHET = "SpecificEpithet";
    public static final String SUBSPECIFIC_EPITHET = "SubspecificEpithet";

    public static final List<String> RANKS_SUPPORTED = Arrays.asList(
            KINGDOM, PHYLUM, CLASS, SUBCLASS, SUPERORDER, ORDER, SUBORDER, INFRAORDER,
            PARVORDER, SUPERFAMILY, FAMILY, SUBFAMILY, GENUS, SUBGENUS, SPECIES,
            SPECIFIC_EPITHET, SUBSPECIFIC_EPITHET);

    public static final String SOURCE_TAXON = "sourceTaxon";
    public static final String NAME_SUFFIX = "Name";
    public static final String SOURCE_TAXON_KINGDOM = SOURCE_TAXON + KINGDOM + NAME_SUFFIX;
    public static final String SOURCE_TAXON_PHYLUM = SOURCE_TAXON + PHYLUM + NAME_SUFFIX;
    public static final String SOURCE_TAXON_CLASS = SOURCE_TAXON + CLASS + NAME_SUFFIX;
    public static final String SOURCE_TAXON_SUBCLASS = SOURCE_TAXON + SUBCLASS + NAME_SUFFIX;
    public static final String SOURCE_TAXON_SUPERORDER = SOURCE_TAXON + SUPERORDER + NAME_SUFFIX;
    public static final String SOURCE_TAXON_ORDER = SOURCE_TAXON + ORDER + NAME_SUFFIX;
    public static final String SOURCE_TAXON_SUBORDER = SOURCE_TAXON + SUBORDER + NAME_SUFFIX;
    public static final String SOURCE_TAXON_INFRAORDER = SOURCE_TAXON + INFRAORDER + NAME_SUFFIX;
    public static final String SOURCE_TAXON_PARVORDER = SOURCE_TAXON + PARVORDER + NAME_SUFFIX;
    public static final String SOURCE_TAXON_SUPERFAMILY = SOURCE_TAXON + SUPERFAMILY + NAME_SUFFIX;
    public static final String SOURCE_TAXON_FAMILY = SOURCE_TAXON + FAMILY + NAME_SUFFIX;
    public static final String SOURCE_TAXON_SUBFAMILY = SOURCE_TAXON + SUBFAMILY + NAME_SUFFIX;
    public static final String SOURCE_TAXON_GENUS = SOURCE_TAXON + GENUS + NAME_SUFFIX;
    public static final String SOURCE_TAXON_SUBGENUS = SOURCE_TAXON + SUBGENUS + NAME_SUFFIX;
    public static final String SOURCE_TAXON_SPECIFIC_EPITHET = SOURCE_TAXON + SPECIFIC_EPITHET + NAME_SUFFIX;
    public static final String SOURCE_TAXON_SUBSPECIFIC_EPITHET = SOURCE_TAXON + SUBSPECIFIC_EPITHET + NAME_SUFFIX;

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
    public static final String TARGET_TAXON_KINGDOM = TARGET_TAXON + KINGDOM + NAME_SUFFIX;
    public static final String TARGET_TAXON_PHYLUM = TARGET_TAXON + PHYLUM + NAME_SUFFIX;
    public static final String TARGET_TAXON_CLASS = TARGET_TAXON + CLASS + NAME_SUFFIX;
    public static final String TARGET_TAXON_SUBCLASS = TARGET_TAXON + SUBCLASS + NAME_SUFFIX;
    public static final String TARGET_TAXON_SUPERORDER = TARGET_TAXON + SUPERORDER + NAME_SUFFIX;
    public static final String TARGET_TAXON_ORDER = TARGET_TAXON + ORDER + NAME_SUFFIX;
    public static final String TARGET_TAXON_SUBORDER = TARGET_TAXON + SUBORDER + NAME_SUFFIX;
    public static final String TARGET_TAXON_INFRAORDER = TARGET_TAXON + INFRAORDER + NAME_SUFFIX;
    public static final String TARGET_TAXON_PARVORDER = TARGET_TAXON + PARVORDER + NAME_SUFFIX;
    public static final String TARGET_TAXON_SUPERFAMILY = TARGET_TAXON + SUPERFAMILY + NAME_SUFFIX;
    public static final String TARGET_TAXON_FAMILY = TARGET_TAXON + FAMILY + NAME_SUFFIX;
    public static final String TARGET_TAXON_SUBFAMILY = TARGET_TAXON + SUBFAMILY + NAME_SUFFIX;
    public static final String TARGET_TAXON_GENUS = TARGET_TAXON + GENUS + NAME_SUFFIX;
    public static final String TARGET_TAXON_SUBGENUS = TARGET_TAXON + SUBGENUS + NAME_SUFFIX;
    public static final String TARGET_TAXON_SPECIFIC_EPITHET = TARGET_TAXON + SPECIFIC_EPITHET + NAME_SUFFIX;
    public static final String TARGET_TAXON_SUBSPECIFIC_EPITHET = TARGET_TAXON + SUBSPECIFIC_EPITHET + NAME_SUFFIX;

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

    public static final String TARGET_TAXON_SPECIES = TARGET_TAXON + "Species" + NAME_SUFFIX;
    public static final String SOURCE_TAXON_SPECIES = SOURCE_TAXON + "Species" + NAME_SUFFIX;

    public static final List<String> TAXON_RANK_NAMES = Arrays.asList("kingdom", "phylum", "class", "order", "family", "genus");

    public static final Pattern PATTERN_TAXON_COLUMN_NAME = Pattern.compile(
            "(target|source){1}" +
                    "(Taxon){0,1}" +
                    "(Common|" + join(RANKS_SUPPORTED, "|") + "){1}" +
                    "(Name|Id){0,1}"
    );
    private static final String SOURCE_TAXON_COMMON_NAME = SOURCE_TAXON + "CommonName";
    private static final String TARGET_TAXON_COMMON_NAME = TARGET_TAXON + "CommonName";

    public static Map<String, String> taxonToMap(Taxon taxon) {
        Map<String, String> properties = new HashMap<>();
        if (taxon != null) {
            putTaxonProperties(taxon, properties);
        }
        return Collections.unmodifiableMap(properties);
    }

    private static void putTaxonProperties(Taxon taxon, Map<String, String> properties) {
        properties.put(NAME, taxon.getName());
        properties.put(RANK, taxon.getRank());
        properties.put(EXTERNAL_ID, taxon.getExternalId());
        properties.put(PATH, taxon.getPath());
        properties.put(PATH_IDS, taxon.getPathIds());
        properties.put(PATH_NAMES, taxon.getPathNames());
        properties.put(COMMON_NAMES, taxon.getCommonNames());
        if (isBlank(taxon.getExternalUrl()) && isNotBlank(taxon.getExternalId())) {
            properties.put(EXTERNAL_URL, urlForExternalId(taxon.getExternalId()));
        } else {
            properties.put(EXTERNAL_URL, taxon.getExternalUrl());
        }

        properties.put(THUMBNAIL_URL, taxon.getThumbnailUrl());
        Term status = taxon.getStatus();
        if (status != null
                && isNotBlank(status.getId())
                && isNotBlank(status.getName())) {
            properties.put(STATUS_ID, status.getId());
            properties.put(STATUS_LABEL, status.getName());
        }

        properties.put(NAME_SOURCE, taxon.getNameSource());
        properties.put(NAME_SOURCE_URL, taxon.getNameSourceURL());
        properties.put(NAME_SOURCE_ACCESSED_AT, taxon.getNameSourceAccessedAt());

        properties.put(AUTHORSHIP, taxon.getAuthorship());
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

        taxon.setAuthorship(properties.get(PropertyAndValueDictionary.AUTHORSHIP));
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
        if (hasPath(taxonA) && hasPath(taxonB)) {
            Map<String, String> pathMapA = toPathNameMap(taxonA, taxonA.getPath());
            Map<String, String> pathMapB = toPathNameMap(taxonB, taxonB.getPath());
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

    public static Map<String, String> toPathNameMap(Taxon taxonA, String pathElements) {
        String[] pathParts = splitPreserveAllTokens(pathElements, CharsetConstant.SEPARATOR_CHAR);
        String[] pathNames = splitPreserveAllTokens(taxonA.getPathNames(), CharsetConstant.SEPARATOR_CHAR);
        return populateMap(pathParts, pathNames);
    }

    private static Map<String, String> populateMap(String[] pathParts, String[] pathNames) {
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
        return hasPath(taxon)
                && isNotBlank(taxon.getName())
                && isNotBlank(taxon.getExternalId());
    }

    private static boolean hasPath(Taxon taxon) {
        return taxon != null
                && isNotBlank(taxon.getPath());
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

    private static String generateTaxonPath(Map<String, String> properties,
                                            List<String> allRanks,
                                            String genusRank,
                                            String specificEpithetRank,
                                            String subspecificEpithetRank) {
        return generateTaxonPath(properties, allRanks, genusRank, specificEpithetRank, subspecificEpithetRank, null);
    }

    private static String generateTaxonPath(Map<String, String> properties,
                                            List<String> allRanks,
                                            String genusRank,
                                            String specificEpithetRank,
                                            String subspecificEpithetRank,
                                            String speciesRank) {
        Stream<String> rankValues = allRanks
                .stream()
                .map(rankName -> getRankValue(properties, rankName))
                .filter(StringUtils::isNotBlank);

        String species = trim(generateSpeciesName(properties, genusRank, specificEpithetRank, subspecificEpithetRank, speciesRank));

        Stream<String> ranksWithSpecies = isBlank(species)
                ? rankValues
                : Stream.concat(rankValues, Stream.of(species));

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
                .map(x -> Pair.of(x, getRankValue(properties, x)))
                .filter(x -> isNotBlank(x.getValue()))
                .map(x -> lowerCase(RegExUtils.replacePattern(replace(x.getKey(), keyPrefix, ""), NAME_SUFFIX + "$", "")));

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


    public static Taxon generateTaxonName(Map<String, String> properties,
                                          List<String> higherOrderRankKeys,
                                          String genusKey,
                                          String specificEpithetKey,
                                          String subspecificEpithetKey,
                                          String speciesKey,
                                          String commonNameKey) {

        String prefix = StringUtils.getCommonPrefix(
                higherOrderRankKeys.toArray(new String[0])
        );

        Taxon taxon = generateSpecies(properties, genusKey, specificEpithetKey, subspecificEpithetKey, speciesKey);

        String taxonName = taxon == null ? null : taxon.getName();

        if (isBlank(taxonName)) {
            String taxonRank = null;
            for (String rankName : higherOrderRankKeys) {
                String name = getRankValue(properties, rankName);

                if (isNotBlank(name)) {
                    taxonName = name;
                    taxonRank = parseRank(rankName, prefix);
                    break;
                }
            }
            if (isBlank(taxonName)) {
                taxonName = getRankValue(properties, commonNameKey);
                taxonRank = parseRank(commonNameKey, prefix);
            }

            if (StringUtils.isNotBlank(taxonName)) {
                taxon = new TaxonImpl(taxonName);
                taxon.setName(taxonName);
                taxon.setRank(taxonRank);
            }
        }

        return taxon;
    }

    public static String parseRank(String rankKey, String prefix) {
        return StringUtils.lowerCase(StringUtils.removeEnd(StringUtils.removeStart(rankKey, prefix), "Name"));
    }

    public static String getRankValue(Map<String, String> properties, String rankName) {
        String name = properties.get(rankName);
        if (isBlank(name) && StringUtils.endsWith(rankName, NAME_SUFFIX)) {
            String rankNameTruncated = rankName.substring(0, rankName.length() - NAME_SUFFIX.length());
            name = properties.get(rankNameTruncated);
        }
        return name;
    }

    public static boolean hasRankValue(Map<String, String> properties, String rankName) {
        String name = properties.get(rankName);
        if (isBlank(name) && !StringUtils.endsWith(rankName, NAME_SUFFIX)) {
            name = properties.get(rankName + NAME_SUFFIX);
        }
        return StringUtils.isNoneBlank(name);
    }

    public static String generateSpeciesName(Map<String, String> properties,
                                             String genusKey,
                                             String specificEpithetKey,
                                             String subspecificEpithetKey,
                                             String speciesKey) {
        return generateSpecies(
                properties,
                genusKey,
                specificEpithetKey,
                subspecificEpithetKey,
                speciesKey
        ).getName();
    }

    public static Taxon generateSpecies(Map<String, String> properties,
                                        String genusKey,
                                        String specificEpithetKey,
                                        String subspecificEpithetKey,
                                        String speciesKey) {
        String speciesName = null;
        if (isNotBlank(genusKey)
                && hasRankValue(properties, genusKey)
                && isNotBlank(specificEpithetKey)
                && hasRankValue(properties, specificEpithetKey)) {
            List<String> speciesNameParts = Arrays.asList(
                    getRankValue(properties, genusKey),
                    getRankValue(properties, specificEpithetKey),
                    getRankValue(properties, subspecificEpithetKey));
            speciesName = trim(join(speciesNameParts, " "));
        } else if (isNotBlank(speciesKey)
                && hasRankValue(properties, speciesKey)) {
            speciesName = trim(getRankValue(properties, speciesKey));
        }
        TaxonImpl taxon = new TaxonImpl(speciesName);
        String[] parts = split(taxon.getName(), " ");
        if (parts != null && parts.length == 2) {
            taxon.setRank(StringUtils.lowerCase(SPECIES));
        } else if (parts != null && parts.length > 2) {
            taxon.setRank(StringUtils.lowerCase("subspecies"));

        }
        return taxon;
    }

    public static String generateSourceTaxonName(Map<String, String> properties) {
        return generateSourceTaxon(properties).getName();
    }

    public static Taxon generateSourceTaxon(Map<String, String> properties) {
        return generateTaxonName(properties,
                SOURCE_TAXON_HIGHER_ORDER_RANK_KEYS,
                SOURCE_TAXON_GENUS,
                SOURCE_TAXON_SPECIFIC_EPITHET,
                SOURCE_TAXON_SUBSPECIFIC_EPITHET,
                SOURCE_TAXON_SPECIES,
                SOURCE_TAXON_COMMON_NAME);
    }

    public static String generateTargetTaxonName(Map<String, String> properties) {
        return generateTargetTaxon(properties).getName();
    }

    public static Taxon generateTargetTaxon(Map<String, String> properties) {
        return generateTaxonName(properties,
                TARGET_TAXON_HIGHER_ORDER_RANK_KEYS,
                TARGET_TAXON_GENUS,
                TARGET_TAXON_SPECIFIC_EPITHET,
                TARGET_TAXON_SUBSPECIFIC_EPITHET,
                TARGET_TAXON_SPECIES,
                TARGET_TAXON_COMMON_NAME);
    }

    public static Map<String, String> enrichTaxonNames(final Map<String, String> properties) {
        Map<String, String> enrichedProperties = new TreeMap<>(properties);
        for (String propertyName : properties.keySet()) {
            String expandedName = expandTaxonColumnNameIfNeeded(propertyName);
            if (!StringUtils.equals(propertyName, expandedName)) {
                enrichedProperties.put(expandedName, properties.get(propertyName));
            }
        }

        return enrichIfNeeded(enrichedProperties);
    }

    public static Map<String, String> enrichIfNeeded(Map<String, String> properties) {
        if (StringUtils.isBlank(properties.get(SOURCE_TAXON_NAME))) {
            Taxon taxon = generateSourceTaxon(properties);
            if (taxon != null) {
                InteractUtil.putIfKeyNotExistsAndValueNotBlank(properties, SOURCE_TAXON_NAME, taxon.getName());
                InteractUtil.putIfKeyNotExistsAndValueNotBlank(properties, SOURCE_TAXON_RANK, taxon.getRank());
                InteractUtil.putIfKeyNotExistsAndValueNotBlank(properties, SOURCE_TAXON_ID, taxon.getExternalId());
            }
        }

        if (StringUtils.isBlank(properties.get(SOURCE_TAXON_PATH))) {
            String path = generateSourceTaxonPath(properties);
            if (isNotBlank(path)) {
                properties.put(SOURCE_TAXON_PATH, path);
                properties.put(SOURCE_TAXON_PATH_NAMES, generateSourceTaxonPathNames(properties));
            }
        }

        if (StringUtils.isBlank(properties.get(SOURCE_TAXON_PATH))
                && StringUtils.isNotBlank(properties.get(SOURCE_TAXON_COMMON_NAME))) {
            properties.put(SOURCE_TAXON_PATH, properties.get(SOURCE_TAXON_COMMON_NAME));
        }

        if (StringUtils.isBlank(properties.get(TARGET_TAXON_NAME))) {
            Taxon taxon = generateTargetTaxon(properties);
            if (taxon != null) {
                InteractUtil.putIfKeyNotExistsAndValueNotBlank(properties, TARGET_TAXON_NAME, taxon.getName());
                InteractUtil.putIfKeyNotExistsAndValueNotBlank(properties, TARGET_TAXON_RANK, taxon.getRank());
                InteractUtil.putIfKeyNotExistsAndValueNotBlank(properties, TARGET_TAXON_ID, taxon.getExternalId());
            }
        }

        if (StringUtils.isBlank(properties.get(TARGET_TAXON_PATH))) {
            String path = generateTargetTaxonPath(properties);
            if (isNotBlank(path)) {
                properties.put(TARGET_TAXON_PATH, path);
                properties.put(TARGET_TAXON_PATH_NAMES, generateTargetTaxonPathNames(properties));
            }
        }

        if (StringUtils.isBlank(properties.get(TARGET_TAXON_PATH))
                && StringUtils.isNotBlank(properties.get(TARGET_TAXON_COMMON_NAME))) {
            properties.put(TARGET_TAXON_PATH, properties.get(TARGET_TAXON_COMMON_NAME));
        }

        if (StringUtils.isBlank(properties.get(TARGET_TAXON_PATH))
                && StringUtils.isNotBlank(properties.get(TARGET_TAXON_COMMON_NAME))) {
            properties.put(TARGET_TAXON_PATH, properties.get(TARGET_TAXON_COMMON_NAME));
        }

        return properties;
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

    public static String expandTaxonColumnNameIfNeeded(String taxonColumnLabel) {
        Matcher matcher = PATTERN_TAXON_COLUMN_NAME.matcher(taxonColumnLabel);
        String normalizedLabel = taxonColumnLabel;
        if (matcher.matches()) {
            String sourceOrTarget = matcher.group(1);
            String taxonPrefix = StringUtils.defaultString(matcher.group(2), "Taxon");
            String rankName = matcher.group(3);
            String nameOrIdSuffix = StringUtils.defaultString(matcher.group(4), "Name");
            normalizedLabel = sourceOrTarget
                    + taxonPrefix
                    + rankName
                    + nameOrIdSuffix;
        }
        return normalizedLabel;
    }
}
