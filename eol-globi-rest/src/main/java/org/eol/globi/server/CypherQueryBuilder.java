package org.eol.globi.server;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelType;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.server.util.InteractionTypeExternal;
import org.eol.globi.server.util.RequestHelper;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.InteractUtil;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eol.globi.server.util.ResultField.INTERACTION_TYPE;
import static org.eol.globi.server.util.ResultField.SOURCE_TAXON_NAME;
import static org.eol.globi.server.util.ResultField.TARGET_TAXON_NAME;
import static org.eol.globi.server.util.ResultField.TAXON_COMMON_NAMES;
import static org.eol.globi.server.util.ResultField.TAXON_EXTERNAL_ID;
import static org.eol.globi.server.util.ResultField.TAXON_NAME;
import static org.eol.globi.server.util.ResultField.TAXON_PATH;
import static org.eol.globi.server.util.ResultField.TAXON_PATH_IDS;
import static org.eol.globi.server.util.ResultField.TAXON_PATH_RANKS;

public class CypherQueryBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(CypherQueryBuilder.class);

    public static final String INTERACTION_PREYS_ON = InteractType.PREYS_UPON.getLabel();
    public static final String INTERACTION_PREYED_UPON_BY = InteractType.PREYED_UPON_BY.getLabel();

    public static final String INTERACTION_EATS = InteractType.ATE.getLabel();
    public static final String INTERACTION_EATEN_BY = InteractType.EATEN_BY.getLabel();

    public static final String INTERACTION_PARASITE_OF = InteractType.PARASITE_OF.getLabel();
    public static final String INTERACTION_HAS_PARASITE = InteractType.HAS_PARASITE.getLabel();

    public static final String INTERACTION_ENDOPARASITE_OF = InteractType.ENDOPARASITE_OF.getLabel();
    public static final String INTERACTION_HAS_ENDOPARASITE = InteractType.HAS_ENDOPARASITE.getLabel();

    public static final String INTERACTION_ECTOPARASITE_OF = InteractType.ECTOPARASITE_OF.getLabel();
    public static final String INTERACTION_HAS_ECTOPARASITE = InteractType.HAS_ECTOPARASITE.getLabel();

    public static final String INTERACTION_PARASITOID_OF = InteractType.PARASITOID_OF.getLabel();
    public static final String INTERACTION_HAS_PARASITOID = InteractType.HAS_PARASITOID.getLabel();

    public static final String INTERACTION_POLLINATES = InteractType.POLLINATES.getLabel();
    public static final String INTERACTION_POLLINATED_BY = InteractType.POLLINATED_BY.getLabel();

    public static final String INTERACTION_PATHOGEN_OF = InteractType.PATHOGEN_OF.getLabel();
    public static final String INTERACTION_HAS_PATHOGEN = InteractType.HAS_PATHOGEN.getLabel();

    public static final String INTERACTION_VECTOR_OF = InteractType.VECTOR_OF.getLabel();
    public static final String INTERACTION_HAS_VECTOR = InteractType.HAS_VECTOR.getLabel();
    public static final String INTERACTION_DISPERSAL_VECTOR_OF = InteractType.DISPERSAL_VECTOR_OF.getLabel();
    public static final String INTERACTION_HAS_DISPERSAL_VECTOR = InteractType.HAS_DISPERAL_VECTOR.getLabel();

    public static final String INTERACTION_HOST_OF = InteractType.HOST_OF.getLabel();
    public static final String INTERACTION_HAS_HOST = InteractType.HAS_HOST.getLabel();

    public static final String INTERACTION_VISITS_FLOWERS_OF = InteractType.VISITS_FLOWERS_OF.getLabel();
    public static final String INTERACTION_FLOWERS_VISITED_BY = InteractType.FLOWERS_VISITED_BY.getLabel();

    public static final String INTERACTION_SYMBIONT_OF = InteractType.SYMBIONT_OF.getLabel();
    public static final String INTERACTION_MUTUALIST_OF = InteractType.MUTUALIST_OF.getLabel();
    public static final String INTERACTION_COMMENSALIST_OF = InteractType.COMMENSALIST_OF.getLabel();
    public static final String INTERACTION_INTERACTS_WITH = InteractType.INTERACTS_WITH.getLabel();
    public static final String INTERACTION_RELATED_TO = InteractType.RELATED_TO.getLabel();
    public static final String INTERACTION_CO_OCCURS_WITH = InteractType.CO_OCCURS_WITH.getLabel();
    public static final String INTERACTION_CO_ROOSTS_WITH = InteractType.CO_ROOSTS_WITH.getLabel();

    public static final String INTERACTION_KILLS = InteractType.KILLS.getLabel();
    public static final String INTERACTION_KILLED_BY = InteractType.KILLED_BY.getLabel();

    public static final String INTERACTION_HAS_HABITAT = InteractType.HAS_HABITAT.getLabel();
    public static final String INTERACTION_CREATES_HABITAT_FOR = InteractType.CREATES_HABITAT_FOR.getLabel();

    public static final Map<String, InteractionTypeExternal> INTERACTION_TYPE_INTERNAL_EXTERNAL_MAP = new TreeMap<String, InteractionTypeExternal>() {
        {
            put(InteractType.ATE.toString(), InteractionTypeExternal.EATS);
            put(InteractType.EATEN_BY.toString(), InteractionTypeExternal.EATEN_BY);
            put(InteractType.PREYS_UPON.toString(), InteractionTypeExternal.PREYS_ON);
            put(InteractType.PREYED_UPON_BY.toString(), InteractionTypeExternal.PREYED_UPON_BY);
            put(InteractType.KILLS.toString(), InteractionTypeExternal.KILLS);
            put(InteractType.KILLED_BY.toString(), InteractionTypeExternal.KILLED_BY);

            put(InteractType.VISITS_FLOWERS_OF.toString(), InteractionTypeExternal.VISITS_FLOWERS_OF);
            put(InteractType.FLOWERS_VISITED_BY.toString(), InteractionTypeExternal.FLOWERS_VISITED_BY);
            put(InteractType.POLLINATES.toString(), InteractionTypeExternal.POLLINATES);
            put(InteractType.POLLINATED_BY.toString(), InteractionTypeExternal.POLLINATED_BY);

            put(InteractType.HAS_HOST.toString(), InteractionTypeExternal.HAS_HOST);
            put(InteractType.HOST_OF.toString(), InteractionTypeExternal.HOST_OF);
            put(InteractType.PARASITE_OF.toString(), InteractionTypeExternal.PARASITE_OF);
            put(InteractType.HAS_PARASITE.toString(), InteractionTypeExternal.HAS_PARASITE);
            put(InteractType.PARASITOID_OF.toString(), InteractionTypeExternal.PARASITOID_OF);
            put(InteractType.HAS_PARASITOID.toString(), InteractionTypeExternal.HAS_PARASITOID);
            put(InteractType.PATHOGEN_OF.toString(), InteractionTypeExternal.PATHOGEN_OF);
            put(InteractType.HAS_PATHOGEN.toString(), InteractionTypeExternal.HAS_PATHOGEN);
            put(InteractType.VECTOR_OF.toString(), InteractionTypeExternal.VECTOR_OF);
            put(InteractType.HAS_VECTOR.toString(), InteractionTypeExternal.HAS_VECTOR);
            put(InteractType.DISPERSAL_VECTOR_OF.toString(), InteractionTypeExternal.DISPERSAL_VECTOR_OF);
            put(InteractType.HAS_DISPERAL_VECTOR.toString(), InteractionTypeExternal.HAS_DISPERSAL_VECTOR);

            put(InteractType.HAS_HABITAT.toString(), InteractionTypeExternal.HAS_HABITAT);
            put(InteractType.CREATES_HABITAT_FOR.toString(), InteractionTypeExternal.CREATES_HABITAT_FOR);


            put(InteractType.SYMBIONT_OF.toString(), InteractionTypeExternal.SYMBIONT_OF);
            put(InteractType.MUTUALIST_OF.toString(), InteractionTypeExternal.MUTUALIST_OF);

            put(InteractType.INTERACTS_WITH.toString(), InteractionTypeExternal.INTERACTS_WITH);
        }
    };

    static final Map<String, String> EMPTY_PARAMS = new TreeMap<String, String>();
    public static final List<ResultField> TAXON_FIELDS = Collections.unmodifiableList(new ArrayList<ResultField>() {{
        add(TAXON_NAME);
        add(TAXON_COMMON_NAMES);
        add(TAXON_EXTERNAL_ID);
        add(TAXON_PATH);
        add(TAXON_PATH_IDS);
        add(TAXON_PATH_RANKS);
    }});

    public static final Map<ResultField, String> FIELD_MAP = Collections.unmodifiableMap(new TreeMap<ResultField, String>() {{
        put(TAXON_NAME, "taxon.name");
        put(TAXON_COMMON_NAMES, "taxon.commonNames");
        put(TAXON_EXTERNAL_ID, "taxon.externalId");
        put(TAXON_PATH, "taxon.path");
        put(TAXON_PATH_IDS, "taxon.pathIds");
        put(TAXON_PATH_RANKS, "taxon.pathNames");
    }});

    public static final long DEFAULT_LIMIT = 1024L;
    public static final String ALL_LOCATIONS_INDEX_SELECTOR = " loc = node:locations('latitude:*')";

    static public CypherQuery createDistinctTaxaInLocationQuery(Map<String, String[]> params) {
        StringBuilder builder = new StringBuilder();
        List<String> interactionTypes = collectParamValues(params, ParamName.INTERACTION_TYPE);

        if (RequestHelper.isSpatialSearch(params)) {
            appendSpatialStartWhereWith(params, builder);
        } else {
            builder.append("START taxon = node:taxons('*:*') ");
        }

        if (RequestHelper.isSpatialSearch(params)) {
            builder.append("MATCH taxon<-[:CLASSIFIED_AS]-specimen-[:COLLECTED_AT]->loc");
            if (!interactionTypes.isEmpty()) {
                builder.append(", taxon-[:");
                builder.append(createInteractionTypeSelector(interactionTypes));
                builder.append("]->otherTaxon ");
            } else {
                builder.append(" ");
            }
        } else {
            if (!interactionTypes.isEmpty()) {
                builder.append("MATCH taxon-[:");
                builder.append(createInteractionTypeSelector(interactionTypes));
                builder.append("]->otherTaxon ");
            }
        }

        List<String> fields = collectRequestedFields(params);
        List<ResultField> returnFields = CypherReturnClauseBuilder.actualReturnFields(fields, TAXON_FIELDS, TAXON_FIELDS);

        for (int i = 0; i < returnFields.size(); i++) {
            ResultField fieldName = returnFields.get(i);
            if (i == 0) {
                builder.append("RETURN distinct(").append(FIELD_MAP.get(fieldName)).append(") as ").append(fieldName);
            } else {
                builder.append(", ").append(FIELD_MAP.get(fieldName)).append(" as ").append(fieldName);
            }
        }
        return new CypherQuery(builder.toString(), new HashMap<>());
    }

    protected static List<String> collectRequestedFields(Map params) {
        List<String> requestedFields = collectParamValues(params, ParamName.FIELD);
        if (requestedFields.isEmpty()) {
            List<String> fields = collectParamValues(params, ParamName.FIELDS);
            if (fields.size() > 0) {
                String[] requestedFieldsSplit = StringUtils.split(fields.get(0), ',');
                if (requestedFieldsSplit != null) {
                    for (String s : requestedFieldsSplit) {
                        requestedFields.add(StringUtils.trim(s));
                    }
                }
            }
        }
        return requestedFields.stream().distinct().collect(Collectors.toList());
    }

    public static String selectorPrefixForName(String name, boolean isExactMatch) {
        String prefix = "path:";
        if (isExactMatch) {
            if (isExternalId(name)) {
                prefix = "externalId:";
            } else {
                prefix = "name:";
            }
        }
        return prefix;
    }

    public static void addLocationClausesIfNecessary(StringBuilder query, Map parameterMap, QueryType queryType) {
        if (parameterMap != null) {
            RequestHelper.appendSpatialClauses(parameterMap, query, queryType);
        }
    }

    public static String lucenePathQuery(List<String> taxonNames, boolean isExactMatch) {
        int count = 0;
        StringBuilder lucenePathQuery = new StringBuilder();
        for (String taxonSelector : taxonNames) {
            if (count > 0) {
                lucenePathQuery.append(" OR ");
            }
            lucenePathQuery
                    .append(selectorPrefixForName(taxonSelector, isExactMatch))
                    .append("\\\"")
                    // see https://stackoverflow.com/questions/25450308/full-text-search-in-neo4j-with-spaces
                    .append(escapeWhitespace(taxonSelector))
                    .append("\\\"");
            count++;
        }
        return lucenePathQuery.toString();
    }

    public static String escapeWhitespace(String taxonSelector) {
        return StringUtils.replace(taxonSelector, " ", "\\\\\\\\ ");
    }

    protected static String strictExternalIdMatch(List<String> terms) {
        List<String> quotedTerms = new ArrayList<String>();
        for (String term : terms) {
            quotedTerms.add("externalId:\\\"" + escapeWhitespace(term) + "\\\"");
        }
        return orNestedTerms(quotedTerms);
    }

    private static String orNestedTerms(List<String> quotedTerms) {
        return StringUtils.join(quotedTerms, " ");
    }

    private static Map<String, String> getParams(List<String> sourceTaxonNames, List<String> targetTaxonNames, boolean exactNameMatchesOnly, Map parameterMap) {
        Map<String, String> paramMap = new HashMap<String, String>();
        if (sourceTaxonNames != null && sourceTaxonNames.size() > 0) {
            paramMap.put(SOURCE_TAXON_NAME.getLabel(), lucenePathQuery(sourceTaxonNames, exactNameMatchesOnly));
        }

        if (targetTaxonNames != null && targetTaxonNames.size() > 0) {
            paramMap.put(TARGET_TAXON_NAME.getLabel(), lucenePathQuery(targetTaxonNames, exactNameMatchesOnly));
        }

        List<String> accordingTo = collectAccordingTo(parameterMap);

        if (accordingTo != null && accordingTo.size() > 0) {
            List<DOI> dois = extractDOIs(accordingTo);
            if (dois.size() > 0) {
                List<String> DOIs = dois.stream().map(DOI::toString).collect(Collectors.toList());
                paramMap.put("accordingTo", matchReferenceOrDataset(DOIs));
            } else if (isAccordingToNamespaceQuery(accordingTo)) {
                List<String> namespaces = getNamespaces(accordingTo);
                paramMap.put("accordingTo", "namespace:\\\"" + orNestedTerms(namespaces) + "\\\"");
            } else {
                paramMap.put("accordingTo", matchReferenceOrDataset(accordingTo));
            }
        }

        List<String> prefix = collectParamValues(parameterMap, ParamName.TAXON_ID_PREFIX);
        if (prefix != null && prefix.size() > 0) {
            String firstPrefix = prefix.get(0) + ".*";
            paramMap.put("source_taxon_prefix", firstPrefix);
            paramMap.put("target_taxon_prefix", firstPrefix);
        }

        return paramMap;
    }

    static List<String> collectAccordingTo(Map parameterMap) {
        List<String> accordingTo = collectParamValues(parameterMap, ParamName.ACCORDING_TO);
        if (accordingTo != null && accordingTo.size() > 0) {
            accordingTo = accordingTo.stream()
                    .map(s -> StringUtils.equalsIgnoreCase(s, "inaturalist")
                            ? "globi:globalbioticinteractions/inaturalist"
                            : s).collect(Collectors.toList());
        }
        return accordingTo;
    }


    static String matchReferenceOrDataset(List<String> accordingTo) {
        List<String> expandedList = new ArrayList<>(accordingTo);
        expandedList.addAll(accordingTo.stream()
                .filter(s -> StringUtils.startsWith(s, "http://gomexsi.tamucc.edu"))
                .map(s -> "http://gomexsi.tamucc.edu/")
                .collect(Collectors.toList()));

        return strictExternalIdMatch(expandedList);
    }

    private static void appendTaxonSelectors(boolean includeSourceTaxon, boolean includeTargetTaxon, StringBuilder query, boolean exactNameMatchesOnly) {
        if (includeSourceTaxon) {
            final String sourceTaxonSelector = "sourceTaxon = " + getTaxonPathSelector(SOURCE_TAXON_NAME.getLabel(), exactNameMatchesOnly);
            query.append(sourceTaxonSelector);
        }
        if (includeTargetTaxon) {
            if (includeSourceTaxon) {
                query.append(", ");
            }
            final String targetTaxonSelector = "targetTaxon = " + getTaxonPathSelector(TARGET_TAXON_NAME.getLabel(), exactNameMatchesOnly);
            query.append(targetTaxonSelector);
        }
    }

    private static String getTaxonPathSelector(String taxonParamName, boolean exactNameMatchesOnly) {
        String prefix = exactNameMatchesOnly ? "node:taxons" : "node:taxonPaths";
        return prefix + "({" + taxonParamName + "})";
    }

    public static CypherQuery shortestPathQuery(final String startTaxon, final String endTaxon) {
        String query = "START startNode = node:taxons(name={startTaxon}),endNode = node:taxons(name={endTaxon}) " +
                "MATCH p = allShortestPaths(startNode-[:" + InteractUtil.allInteractionsCypherClause() + "|CLASSIFIED_AS*..100]-endNode) " +
                "RETURN extract(n in (filter(x in nodes(p) where exists(x.name))) | n.name ) as shortestPaths ";


        HashMap<String, String> params = new HashMap<String, String>() {{
            put("startTaxon", startTaxon);
            put("endTaxon", endTaxon);
        }};

        return new CypherQuery(query, params);
    }

    public static CypherQuery externalIdForStudy(final String studyTitle) {
        String query = "START study = node:studies(title={studyTitle}) " +
                " WHERE exists(study.externalId) RETURN study.externalId as study";

        HashMap<String, String> params = new HashMap<String, String>() {{
            put("studyTitle", studyTitle);
        }};

        return new CypherQuery(query, params);
    }

    public static CypherQuery externalIdForTaxon(final String taxonName) {
        String query = "START taxon = node:taxons(name={taxonName}) " +
                " WHERE exists(taxon.externalId) RETURN taxon.externalId as externalId";

        HashMap<String, String> taxonName1 = new HashMap<String, String>() {{
            put("taxonName", taxonName);
        }};

        return new CypherQuery(query, taxonName1);
    }

    public static CypherQuery locations() {
        return locations(null);
    }

    public static CypherQuery locations(Map parameterMap) {
        StringBuilder query = new StringBuilder();
        final List<String> accordingTo = collectAccordingTo(parameterMap);
        if (accordingTo != null && accordingTo.size() > 0) {
            appendStartClause2(parameterMap, Collections.<String>emptyList(), Collections.<String>emptyList(), query);
            query.append(" MATCH study-[:" + createArgumentSelector(parameterMap) + "]->specimen-[:COLLECTED_AT]->location");
            query.append(" WITH DISTINCT(location) as loc");
        } else {
            query.append("START " + ALL_LOCATIONS_INDEX_SELECTOR);
        }
        query.append(" RETURN loc.latitude as latitude, loc.longitude as longitude, loc.footprintWKT as footprintWKT");
        return new CypherQuery(query.toString(), getParams(null, null, false, parameterMap));
    }

    public static CypherQuery buildInteractionQuery(final String sourceTaxonName, final String interactionType, final String targetTaxonName, final Map parameterMap, QueryType queryType) {
        final Map paramMapModified = new TreeMap<ParamName, String[]>(parameterMap);
        List<String> sourceTaxa = new ArrayList<String>() {{
            if (sourceTaxonName != null) {
                add(sourceTaxonName);
                paramMapModified.put(ParamName.SOURCE_TAXON.name(), new String[]{sourceTaxonName});
            }
        }};
        List<String> targetTaxa = new ArrayList<String>() {{
            if (targetTaxonName != null) {
                add(targetTaxonName);
                paramMapModified.put(ParamName.TARGET_TAXON.name(), new String[]{targetTaxonName});
            }
        }};
        return buildInteractionQuery(sourceTaxa, interactionType, targetTaxa, paramMapModified, queryType);
    }

    public static CypherQuery buildInteractionQuery(List<String> sourceTaxonName, final String interactionType, List<String> targetTaxonName, Map parameterMap, QueryType queryType) {
        List<String> interactionTypes = new ArrayList<String>() {{
            add(interactionType);
        }};
        return interactionObservations(sourceTaxonName, interactionTypes, targetTaxonName, parameterMap, queryType, getParams(sourceTaxonName, targetTaxonName, shouldIncludeExactNameMatchesOnly(parameterMap), parameterMap));
    }

    protected static CypherQuery interactionObservations(List<String> sourceTaxa, List<String> interactionTypes, List<String> targetTaxa, Map parameterMap, QueryType queryType, Map<String, String> cypherParams) {
        StringBuilder query = appendStartMatchWhereClauses(sourceTaxa, interactionTypes, targetTaxa, parameterMap, queryType);
        CypherReturnClauseBuilder.appendReturnClauseMap(query, queryType, parameterMap);
        return new CypherQuery(query.toString(), cypherParams);
    }


    public static CypherQuery buildInteractionTypeQuery(Map parameterMap) {
        final List<String> taxa = collectParamValues(parameterMap, ParamName.TAXON);
        String query = "START taxon = " + getTaxonPathSelector(TAXON_NAME.getLabel(), false)
                + " MATCH taxon-[rel:" + InteractUtil.allInteractionsCypherClause() + "]->otherTaxon RETURN distinct(type(rel)) as " + INTERACTION_TYPE;
        return new CypherQuery(query
                , new HashMap<String, String>() {
            {
                put(TAXON_NAME.getLabel(), lucenePathQuery(taxa, false));
            }
        });
    }


    public static CypherQuery buildInteractionQuery(Map parameterMap, QueryType queryType) {
        List<String> sourceTaxa = collectParamValues(parameterMap, ParamName.SOURCE_TAXON);
        List<String> targetTaxa = collectParamValues(parameterMap, ParamName.TARGET_TAXON);
        List<String> interactionTypeSelectors = collectParamValues(parameterMap, ParamName.INTERACTION_TYPE);
        Map<String, String> cypherParams = getParams(parameterMap);
        return interactionObservations(sourceTaxa, interactionTypeSelectors, targetTaxa, parameterMap, queryType, cypherParams);
    }

    private static Map<String, String> getParams(Map parameterMap) {
        return getParams(collectParamValues(parameterMap, ParamName.SOURCE_TAXON),
                collectParamValues(parameterMap, ParamName.TARGET_TAXON),
                shouldIncludeExactNameMatchesOnly(parameterMap),
                parameterMap);
    }


    protected static StringBuilder appendTaxonWhereClauseIfNecessary(Map parameterMap, List<String> sourceTaxa, List<String> targetTaxa, StringBuilder query) {
        boolean spatialSearch = RequestHelper.isSpatialSearch(parameterMap);
        boolean exactNameMatchesOnly = shouldIncludeExactNameMatchesOnly(parameterMap);
        if (hasAccordingTo(parameterMap)) {
            appendAndOrWhere(targetTaxa, query, spatialSearch);
            appendTaxonSelector(query, "targetTaxon", targetTaxa, exactNameMatchesOnly);

            appendAndOrWhere(sourceTaxa, query, spatialSearch || targetTaxa.size() > 0);
            appendTaxonSelector(query, "sourceTaxon", sourceTaxa, exactNameMatchesOnly);
        } else if (sourceTaxa.size() > 0) {
            appendAndOrWhere(targetTaxa, query, spatialSearch);
            appendTaxonSelector(query, "targetTaxon", targetTaxa, exactNameMatchesOnly);
        }
        return query;
    }

    private static boolean hasAccordingTo(Map parameterMap) {
        List<String> accordingTo = collectAccordingTo(parameterMap);
        return accordingTo != null && accordingTo.size() > 0;
    }

    private static boolean shouldIncludeExactNameMatchesOnly(Map parameterMap) {
        List<String> trueValues = Arrays.asList("t", "T", "true", "TRUE");
        boolean b = CollectionUtils.containsAny(collectParamValues(parameterMap, ParamName.EXACT_NAME_MATCH_ONLY), trueValues);
        return b || CollectionUtils.containsAny(collectParamValues(parameterMap, ParamName.EXCLUDE_CHILD_TAXA), trueValues);
    }

    private static void appendAndOrWhere(List<String> taxa, StringBuilder query, boolean hasWhereClause) {
        if (taxa.size() > 0) {
            if (hasWhereClause) {
                query.append("AND ");
            } else {
                query.append(" WHERE ");
            }
        }
    }

    protected static StringBuilder appendStartClause2(Map parameterMap, List<String> sourceTaxa, List<String> targetTaxa, StringBuilder query) {
        query.append("START");
        if (hasAccordingTo(parameterMap)) {
            appendWithStudy(query, collectAccordingTo(parameterMap));
        } else if (noSearchCriteria(RequestHelper.isSpatialSearch(parameterMap), sourceTaxa, targetTaxa)) {
            List<String> strings = collectRequestedFields(parameterMap);
            Stream<String> withoutTaxonOrInteractionTypes = strings
                    .stream()
                    .filter(name -> StringUtils.contains(name, "_taxon_"))
                    .filter(name -> StringUtils.contains(name, "interaction_type"));
            if (strings.isEmpty() || withoutTaxonOrInteractionTypes.findFirst().isPresent()) {
                query.append(" study = node:studies('*:*')");
            } else {
                query.append(" sourceTaxon = node:taxons('*:*')");
            }
        } else if (sourceTaxa.size() == 0 && targetTaxa.size() == 0) {
            query.append(ALL_LOCATIONS_INDEX_SELECTOR);
        } else {
            boolean exactNameMatchesOnly = shouldIncludeExactNameMatchesOnly(parameterMap);
            if (sourceTaxa.size() > 0) {
                query.append(" ");
                appendTaxonSelectors(true, false, query, exactNameMatchesOnly);
            } else if (targetTaxa.size() > 0) {
                query.append(" ");
                appendTaxonSelectors(false, true, query, exactNameMatchesOnly);
            }
        }
        return query;
    }

    private static void appendWithStudy(StringBuilder query, List<String> accordingToParams) {
        if (isAccordingToNamespaceQuery(accordingToParams)) {
            query.append(" dataset = node:datasets({accordingTo})")
                    .append(" MATCH study-[:IN_DATASET]->dataset")
                    .append(" WITH study");
        } else {
            query.append(" externalId = node:externalIds({accordingTo})")
                    .append(" MATCH")
                    .append(" x-[:IN_DATASET|HAS_DOI|HAS_EXTERNAL_ID*]->externalId")
                    .append(" WHERE")
                    .append(" x.type = 'StudyNode'")
                    .append(" WITH x as study ");
        }
    }

    private static List<DOI> extractDOIs(List<String> accordingToParams) {
        List<DOI> dois = new ArrayList<>();
        for (String accordingToParam : accordingToParams) {
            try {
                DOI doi = DOI.create(accordingToParam);
                dois.add(doi);
            } catch (MalformedDOIException e) {
                //
            }
        }
        return dois;
    }

    private static boolean isAccordingToNamespaceQuery(List<String> accordingToParams) {
        List<String> namespaceList = getNamespaces(accordingToParams);
        return namespaceList.size() == accordingToParams.size();
    }

    private static List<String> getNamespaces(List<String> accordingToParams) {
        Stream<String> namespaces = accordingToParams.stream()
                .filter(accordingTo -> StringUtils.startsWith(accordingTo, "globi:"))
                .map(accordingTo -> StringUtils.replaceOnce(accordingTo, "globi:", ""))
                // since lucene 4.0 forward slashes need to be escaped
                // see https://stackoverflow.com/questions/17798300/lucene-queryparser-with-in-query-criteria
                // see https://lucene.apache.org/core/4_0_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Regexp_Searches
                .map(accordingTo -> StringUtils.replace(accordingTo, "/", "\\/"));
        return namespaces.collect(Collectors.toList());
    }

    private static boolean hasAtLeastOneURL(List<String> accordingToParams) {
        boolean hasAtLeastOneURL = false;
        for (String s : accordingToParams) {
            UrlValidator urlValidator = new UrlValidator();
            hasAtLeastOneURL = hasAtLeastOneURL || urlValidator.isValid(s);
        }
        return hasAtLeastOneURL;
    }

    private static StringBuilder appendStartMatchWhereClauses(List<String> sourceTaxa, List<String> interactionTypes, List<String> targetTaxa, Map parameterMap, QueryType queryType) {
        StringBuilder query = new StringBuilder();
        appendStartClause2(parameterMap, sourceTaxa, targetTaxa, query);
        if (QueryType.MULTI_TAXON_DISTINCT_BY_NAME_ONLY == queryType) {
            String interactionMatch = "MATCH sourceTaxon-[interaction:" + createInteractionTypeSelector(interactionTypes) + "]->targetTaxon ";
            query
                    .append(" ")
                    .append(interactionMatch);
        } else {
            appendMatchAndWhereClause(interactionTypes, parameterMap, query, queryType);
        }
        StringBuilder stringBuilder = appendTaxonWhereClauseIfNecessary(parameterMap, sourceTaxa, targetTaxa, query);
        if (!RequestHelper.isSpatialSearch(parameterMap) && (QueryType.MULTI_TAXON_ALL.equals(queryType)
                || QueryType.SINGLE_TAXON_ALL.equals(queryType))) {
            query.append(" OPTIONAL MATCH sourceSpecimen-[:COLLECTED_AT]->loc ");
        }
        return stringBuilder;
    }

    protected static StringBuilder appendMatchAndWhereClause(List<String> interactionTypes, Map parameterMap, StringBuilder query, QueryType queryType) {
        String interactionMatch = getInteractionMatch(createInteractionTypeSelector(interactionTypes), createArgumentSelector(parameterMap));
        query.append(" ")
                .append(interactionMatch);
        addLocationClausesIfNecessary(query, parameterMap, queryType);
        return query;
    }

    private static String createArgumentSelector(Map parameterMap) {
        final List<RelType> argumentTypes = QueryType.argumentTypes(parameterMap);
        return argumentTypes
                .stream()
                .map(RelType::name)
                .collect(Collectors.joining("|"));
    }

    private static String getInteractionMatch(String interactionTypeSelector, String studyRel) {
        return "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + interactionTypeSelector + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, " +
                "sourceSpecimen<-[collected_rel:" + studyRel + "]-study-[:IN_DATASET]->dataset";
    }


    protected static String createInteractionTypeSelector(List<String> interactionTypeSelectors) {
        TreeSet<InteractType> cypherTypes = new TreeSet<InteractType>();
        for (String type : interactionTypeSelectors) {
            if (StringUtils.isNotBlank(type)) {
                InteractType interactType = InteractType.typeOf(type);
                if (interactType == null) {
                    throw new IllegalArgumentException("unsupported interaction type [" + type + "]");
                } else {
                    cypherTypes.addAll(InteractType.typesOf(interactType));
                }
            }
        }
        if (cypherTypes.isEmpty()) {
            cypherTypes.addAll(InteractType.typesOf(InteractType.RELATED_TO));
        }
        return StringUtils.join(cypherTypes, "|");
    }

    private static boolean noSearchCriteria(boolean spatialSearch, List<String> sourceTaxaSelectors, List<String> targetTaxaSelectors) {
        return !spatialSearch && sourceTaxaSelectors.size() == 0 && targetTaxaSelectors.size() == 0;
    }

    private static void appendTaxonSelector(StringBuilder query, String taxonLabel, List<String> taxonNames, boolean exactNameMatchesOnly) {
        if (taxonNames.size() > 0) {
            if (exactNameMatchesOnly) {
                List<String> ids = new ArrayList<String>();
                List<String> names = new ArrayList<String>();
                for (String taxonName : taxonNames) {
                    if (isExternalId(taxonName)) {
                        ids.add(taxonName);
                    } else {
                        names.add(taxonName);
                    }
                }
                if (names.size() > 0) {
                    appendNameWhereClause(query, taxonLabel, names, "name");
                }
                if (ids.size() > 0) {
                    if (names.size() > 0) {
                        query.append(" OR ");
                    }
                    appendNameWhereClause(query, taxonLabel, ids, "externalId");
                }
            } else {
                query.append("(exists(").append(taxonLabel).append(".externalIds) AND ANY(x IN split(")
                        .append(taxonLabel).append(".externalIds, '|') WHERE trim(x) in [");
                query.append(StringUtils.join(taxonNames.stream().map(x -> "'" + x + "'").collect(Collectors.toList()), ","));
                query.append("])) ");
            }
        }
    }

    private static void appendNameWhereClause(StringBuilder query, String taxonLabel, List<String> taxonNames, String property) {
        query.append("(exists(").append(taxonLabel).append(".").append(property).append(") AND ").append(taxonLabel).append(".").append(property).append(" IN ['").append(StringUtils.join(taxonNames, "','")).append("']) ");
    }

    private static boolean isExternalId(String taxonName) {
        return StringUtils.contains(taxonName, ":")
                && ExternalIdUtil.isSupported(taxonName);
    }

    protected static List<String> collectParamValues(Map parameterMap, ParamName name) {
        List<String> paramValues = new ArrayList<String>();
        if (parameterMap != null && parameterMap.containsKey(name.getName())) {
            Object paramObject = parameterMap.get(name.getName());
            if (paramObject instanceof String[]) {
                Collections.addAll(paramValues, (String[]) paramObject);
            } else if (paramObject instanceof String) {
                paramValues.add((String) paramObject);
            }
        }
        return paramValues.stream()
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .collect(Collectors.toList());
    }

    public static CypherQuery createPagedQuery(HttpServletRequest request, CypherQuery query) {
        return createPagedQuery(request, query, DEFAULT_LIMIT);
    }

    public static CypherQuery createPagedQuery(HttpServletRequest request, CypherQuery query, long defaultLimit) {
        long defaultValue = 0L;
        long offset = getPagedQueryLongProperty(request, "offset", defaultValue);
        if (offset == defaultValue) {
            offset = getPagedQueryLongProperty(request, "skip", defaultValue);
        }
        long limit = getPagedQueryLongProperty(request, "limit", defaultLimit);
        return createPagedQuery(query, offset, limit);
    }

    public static CypherQuery createPagedQuery(CypherQuery query, long offset, long limit) {
        return new CypherQuery(query.getQuery() + " SKIP " + offset + " LIMIT " + limit, query.getParams(), query.getVersion());
    }

    static long getPagedQueryLongProperty(HttpServletRequest request, String paramName, long defaultValue) {
        long offset = defaultValue;
        if (request != null) {
            String offsetValue = request.getParameter(paramName);
            offset = parsePagedQueryLongValue(paramName, offset, offsetValue);
        }
        return offset;
    }

    static long parsePagedQueryLongValue(String paramName, long offset, String value) {
        if (StringUtils.isNotBlank(value)) {
            try {
                String valLowerCase = StringUtils.lowerCase(value);
                String valTranslatePlusE = StringUtils.replace(valLowerCase, "+e", "e+");
                String valTranslateMinusE = StringUtils.replace(valTranslatePlusE, "-e", "e-");
                BigDecimal bigDecimal = new BigDecimal(valTranslateMinusE);
                offset = bigDecimal.toBigInteger().longValue();
                if (offset < 0) {
                    throw new NumberFormatException("expected positive number, not [" + value + "]");
                }
            } catch (NumberFormatException ex) {
                String o = "malformed query value [" + paramName + "] found: [" + value + "]. Expected some positive integer value (e.g., 1, 2, 400, 1000).";
                LOG.warn(o, ex);
                throw new IllegalArgumentException(o, ex);
            }
        }
        return offset;
    }

    public static CypherQuery spatialInfo(Map<String, String[]> parameterMap) {
        final String interactionLabel = "sourceTaxon.name + type(interact) + targetTaxon.name";
        StringBuilder query = new StringBuilder();

        if (RequestHelper.isSpatialSearch(parameterMap)) {
            appendSpatialStartWhereWith(parameterMap, query);
        } else {
            query.append("START study = node:studies('*:*') ");
        }

        query.append("MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:")
                .append(createArgumentSelector(parameterMap))
                .append("]-study-[:IN_DATASET]->dataset")
                .append(", sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon");
        if (RequestHelper.isSpatialSearch(parameterMap)) {
            query.append(", sourceSpecimen-[:COLLECTED_AT]->loc");
        }
        query.append(" WHERE not(exists(interact.");
        query.append(PropertyAndValueDictionary.INVERTED);
        query.append("))");

        Map<String, String> params = addSourceWhereClause(parameterMap, query);

        query.append(" RETURN count(distinct(study)) as `number of distinct studies`")
                .append(", count(interact) as `number of interactions`")
                .append(", count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`")
                .append(", count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`")
                .append(", count(distinct(dataset)) as `number of distinct study sources`")
                .append(", count(c." + SpecimenConstant.DATE_IN_UNIX_EPOCH + ") as `number of interactions with timestamp`")
        ;
        if (RequestHelper.isSpatialSearch(parameterMap)) {
            query.append(", count(distinct(loc))");
        } else {
            query.append(", NULL");
        }
        query.append(" as `number of distinct locations`")
                .append(", count(distinct(").append(interactionLabel).append(")) as `number of distinct interactions`");
        return new CypherQuery(query.toString(), params);
    }

    public static void appendSpatialStartWhereWith(Map<String, String[]> parameterMap, StringBuilder query) {
        query.append("START loc = node:locations('latitude:*') WHERE ");
        RequestHelper.addSpatialWhereClause(RequestHelper.parseSpatialSearchParams(parameterMap), query);
        query.append("WITH loc ");
    }

    protected static Map<String, String> addSourceWhereClause(Map<String, String[]> parameterMap, StringBuilder query) {
        String[] sourceList = parameterMap == null ? null : parameterMap.get("source");
        final String source = sourceList != null && sourceList.length > 0 ? sourceList[0] : null;
        String sourceWhereClause = StringUtils.isBlank(source) ? "" : " dataset.citation = {source}";
        Map<String, String> params = StringUtils.isBlank(source) ? EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};

        if (StringUtils.isNotBlank(sourceWhereClause)) {
            query.append(" AND");
            query.append(sourceWhereClause);
        }
        return params;
    }

}
