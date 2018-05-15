package org.eol.globi.server;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.UrlValidator;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.SpecimenConstant;
import org.eol.globi.server.util.InteractionTypeExternal;
import org.eol.globi.server.util.RequestHelper;
import org.eol.globi.server.util.ResultField;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.InteractUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
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
    private static final Log LOG = LogFactory.getLog(CypherQueryBuilder.class);

    public static final String INTERACTION_PREYS_ON = InteractType.PREYS_UPON.getLabel();
    public static final String INTERACTION_PREYED_UPON_BY = InteractType.PREYED_UPON_BY.getLabel();

    public static final String INTERACTION_EATS = InteractType.ATE.getLabel();
    public static final String INTERACTION_EATEN_BY = InteractType.EATEN_BY.getLabel();

    public static final String INTERACTION_PARASITE_OF = InteractType.PARASITE_OF.getLabel();
    public static final String INTERACTION_HAS_PARASITE = InteractType.HAS_PARASITE.getLabel();

    public static final String INTERACTION_POLLINATES = InteractType.POLLINATES.getLabel();
    public static final String INTERACTION_POLLINATED_BY = InteractType.POLLINATED_BY.getLabel();

    public static final String INTERACTION_PATHOGEN_OF = InteractType.PATHOGEN_OF.getLabel();
    public static final String INTERACTION_HAS_PATHOGEN = InteractType.HAS_PATHOGEN.getLabel();
    ;

    public static final String INTERACTION_VECTOR_OF = InteractType.VECTOR_OF.getLabel();
    public static final String INTERACTION_HAS_VECTOR = InteractType.HAS_VECTOR.getLabel();
    public static final String INTERACTION_DISPERSAL_VECTOR_OF = InteractType.DISPERSAL_VECTOR_OF.getLabel();
    public static final String INTERACTION_HAS_DISPERSAL_VECTOR = InteractType.HAS_DISPERAL_VECTOR.getLabel();

    public static final String INTERACTION_HOST_OF = InteractType.HOST_OF.getLabel();
    public static final String INTERACTION_HAS_HOST = InteractType.HAS_HOST.getLabel();

    public static final String INTERACTION_VISITS_FLOWERS_OF = InteractType.VISITS_FLOWERS_OF.getLabel();
    public static final String INTERACTION_FLOWERS_VISITED_BY = InteractType.FLOWERS_VISITED_BY.getLabel();

    public static final String INTERACTION_SYMBIONT_OF = InteractType.SYMBIONT_OF.getLabel();
    public static final String INTERACTION_INTERACTS_WITH = InteractType.INTERACTS_WITH.getLabel();

    public static final String INTERACTION_KILLS = InteractType.KILLS.getLabel();
    public static final String INTERACTION_KILLED_BY = InteractType.KILLED_BY.getLabel();

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
            put(InteractType.PATHOGEN_OF.toString(), InteractionTypeExternal.PATHOGEN_OF);
            put(InteractType.HAS_PATHOGEN.toString(), InteractionTypeExternal.HAS_PATHOGEN);
            put(InteractType.VECTOR_OF.toString(), InteractionTypeExternal.VECTOR_OF);
            put(InteractType.HAS_VECTOR.toString(), InteractionTypeExternal.HAS_VECTOR);
            put(InteractType.DISPERSAL_VECTOR_OF.toString(), InteractionTypeExternal.DISPERSAL_VECTOR_OF);
            put(InteractType.HAS_DISPERAL_VECTOR.toString(), InteractionTypeExternal.HAS_DISPERSAL_VECTOR);


            put(InteractType.SYMBIONT_OF.toString(), InteractionTypeExternal.SYMBIONT_OF);

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
                builder.append("RETURN distinct(").append(FIELD_MAP.get(fieldName)).append("?) as ").append(fieldName);
            } else {
                builder.append(", ").append(FIELD_MAP.get(fieldName)).append("? as ").append(fieldName);
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
        return requestedFields;
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
                    .append(taxonSelector)
                    .append("\\\"");
            count++;
        }
        return lucenePathQuery.toString();
    }

    public static String regexWildcard(List<String> terms) {
        return ".*" + regexStrict(terms) + ".*";
    }

    protected static String regexStrict(List<String> terms) {
        return regexStrict(terms, true);
    }

    protected static String regexStrict(List<String> terms, boolean isPartOfLuceneQuery) {
        List<String> quotedTerms = new ArrayList<String>();
        for (String term : terms) {
            String quote = Pattern.quote(term);
            quotedTerms.add(isPartOfLuceneQuery
                    ? quote.replace("\\Q", "\\\\Q").replace("\\E", "\\\\E").replace("\"", "\\\"")
                    : quote);
        }
        return orNestedTerms(quotedTerms);
    }

    private static String orNestedTerms(List<String> quotedTerms) {
        return "(" + StringUtils.join(quotedTerms, "|") + ")";
    }

    private static Map<String, String> getParams(List<String> sourceTaxonNames, List<String> targetTaxonNames, boolean exactNameMatchesOnly, Map parameterMap) {
        Map<String, String> paramMap = new HashMap<String, String>();
        if (sourceTaxonNames != null && sourceTaxonNames.size() > 0) {
            paramMap.put(SOURCE_TAXON_NAME.getLabel(), lucenePathQuery(sourceTaxonNames, exactNameMatchesOnly));
        }

        if (targetTaxonNames != null && targetTaxonNames.size() > 0) {
            paramMap.put(TARGET_TAXON_NAME.getLabel(), lucenePathQuery(targetTaxonNames, exactNameMatchesOnly));
        }

        List<String> accordingTo = collectParamValues(parameterMap, ParamName.ACCORDING_TO);
        if (accordingTo != null && accordingTo.size() > 0) {
            if (isAccordingToNamespaceQuery(accordingTo)) {
                List<String> namespaces = getNamespaces(accordingTo);
                paramMap.put("accordingTo", "namespace:" + orNestedTerms(namespaces));
            } else {
                paramMap.put("accordingTo", regexForAccordingTo(accordingTo));
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

    static String regexForAccordingTo(List<String> accordingTo) {
        List<String> expandedList = new ArrayList<>(accordingTo);
        expandedList.addAll(accordingTo.stream()
                .filter(s -> StringUtils.startsWith(s, "http://gomexsi.tamucc.edu"))
                .map(s -> "http://gomexsi.tamucc.edu.")
                .collect(Collectors.toList()));

        return hasAtLeastOneURL(expandedList) ? regexStrict(expandedList) : regexWildcard(expandedList);
    }

    static void appendTaxonSelectors(boolean includeSourceTaxon, boolean includeTargetTaxon, StringBuilder query, boolean exactNameMatchesOnly) {
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
                "RETURN extract(n in (filter(x in nodes(p) : has(x.name))) : " +
                "coalesce(n.name?)) as shortestPaths ";


        HashMap<String, String> params = new HashMap<String, String>() {{
            put("startTaxon", startTaxon);
            put("endTaxon", endTaxon);
        }};

        return new CypherQuery(query, params);
    }

    public static CypherQuery externalIdForStudy(final String studyTitle) {
        String query = "START study = node:studies(title={studyTitle}) " +
                " RETURN study.externalId? as study";

        HashMap<String, String> params = new HashMap<String, String>() {{
            put("studyTitle", studyTitle);
        }};

        return new CypherQuery(query, params);
    }

    public static CypherQuery externalIdForTaxon(final String taxonName) {
        String query = "START taxon = node:taxons(name={taxonName}) " +
                " RETURN taxon.externalId? as externalId";

        HashMap<String, String> taxonName1 = new HashMap<String, String>() {{
            put("taxonName", taxonName);
        }};

        return new CypherQuery(query, taxonName1);
    }

    public static CypherQuery references(final String source) {
        String whereClause = StringUtils.isBlank(source) ? "" : " AND study.source = {source}";
        Map<String, String> params = StringUtils.isBlank(source) ? EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};
        String cypherQuery = "START study=node:studies('*:*')" +
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:" + InteractUtil.allInteractionsCypherClause() + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                " WHERE not(has(interact." + PropertyAndValueDictionary.INVERTED + "))" + whereClause +
                " RETURN study.institution?, study.period?, study.description?, study.contributor?, count(interact), count(distinct(sourceTaxon.name)), count(distinct(targetTaxon.name)), study.title, study.citation?, study.doi?, study.source, study.externalId?";

        return new CypherQuery(cypherQuery, params);
    }

    public static CypherQuery locations() {
        return locations(null);
    }

    public static CypherQuery locations(Map parameterMap) {
        StringBuilder query = new StringBuilder();
        final List<String> accordingTo = collectParamValues(parameterMap, ParamName.ACCORDING_TO);
        if (accordingTo.size() > 0) {
            appendStartClause2(parameterMap, Collections.<String>emptyList(), Collections.<String>emptyList(), query);
            query.append(" MATCH study-[:COLLECTED]->specimen-[:COLLECTED_AT]->location");
            query.append(" WITH DISTINCT(location) as loc");
        } else {
            query.append("START " + ALL_LOCATIONS_INDEX_SELECTOR);
        }
        query.append(" RETURN loc.latitude? as latitude, loc.longitude? as longitude, loc.footprintWKT? as footprintWKT");
        return new CypherQuery(query.toString(), getParams(null, null, false, parameterMap));
    }

    public static CypherQuery buildInteractionQuery(final String sourceTaxonName, final String interactionType, final String targetTaxonName, final Map parameterMap, QueryType queryType) {
        final Map paramMapModified = new HashMap(parameterMap);
        List<String> sourceTaxa = new ArrayList<String>() {{
            if (sourceTaxonName != null) {
                add(sourceTaxonName);
                paramMapModified.put(ParamName.SOURCE_TAXON, new String[]{sourceTaxonName});
            }
        }};
        List<String> targetTaxa = new ArrayList<String>() {{
            if (targetTaxonName != null) {
                add(targetTaxonName);
                paramMapModified.put(ParamName.TARGET_TAXON, new String[]{targetTaxonName});
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
        return collectParamValues(parameterMap, ParamName.ACCORDING_TO).size() > 0;
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
        List<String> accordingToParams = collectParamValues(parameterMap, ParamName.ACCORDING_TO);
        if (accordingToParams.size() > 0) {
            appendWithStudy(query, accordingToParams);
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
            String whereClause;
            if (hasAtLeastOneURL(accordingToParams)) {
                whereClause = "(has(study.externalId) AND study.externalId =~ {accordingTo})";
            } else {
                whereClause = "(has(study.externalId) AND study.externalId =~ {accordingTo}) OR (has(study.citation) AND study.citation =~ {accordingTo}) OR (has(study.source) AND study.source =~ {accordingTo})";
            }
            query.append(" study = node:studies('*:*') WHERE ")
                    .append(whereClause)
                    .append(" WITH study");
        }
    }

    private static boolean isAccordingToNamespaceQuery(List<String> accordingToParams) {
        List<String> namespaceList = getNamespaces(accordingToParams);
        return namespaceList.size() == accordingToParams.size();
    }

    private static List<String> getNamespaces(List<String> accordingToParams) {
        Stream<String> namespaces = accordingToParams.stream()
                .filter(accordingTo -> StringUtils.startsWith(accordingTo, "globi:"))
                .map(accordingTo -> StringUtils.replaceOnce(accordingTo, "globi:", ""));
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
        return appendTaxonWhereClauseIfNecessary(parameterMap, sourceTaxa, targetTaxa, query);
    }

    protected static StringBuilder appendMatchAndWhereClause(List<String> interactionTypes, Map parameterMap, StringBuilder query, QueryType queryType) {
        String interactionMatch = getInteractionMatch(createInteractionTypeSelector(interactionTypes));
        query.append(" ")
                .append(interactionMatch);
        addLocationClausesIfNecessary(query, parameterMap, queryType);
        return query;
    }

    private static String getInteractionMatch(String interactionTypeSelector) {
        return "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interaction:" + interactionTypeSelector + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen<-[collected_rel:COLLECTED]-study";
    }

    public static CypherQuery sourcesQuery() {
        String cypherQuery = "START study=node:studies('*:*')" +
                " RETURN distinct(study.source)";
        return new CypherQuery(cypherQuery, EMPTY_PARAMS);
    }


    protected static String createInteractionTypeSelector(List<String> interactionTypeSelectors) {
        List<InteractType> cypherTypes = new ArrayList<InteractType>();
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
            cypherTypes.addAll(InteractType.typesOf(InteractType.INTERACTS_WITH));
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
                query.append("(has(").append(taxonLabel).append(".externalIds) AND ").append(taxonLabel).append(".externalIds =~ '(.*(");
                query.append(StringUtils.join(taxonNames, "|"));
                query.append(").*)') ");
            }
        }
    }

    private static void appendNameWhereClause(StringBuilder query, String taxonLabel, List<String> taxonNames, String property) {
        query.append("(has(").append(taxonLabel).append("." + property + ") AND ");
        query.append(taxonLabel).append("." + property + " IN ['").append(StringUtils.join(taxonNames, "','")).append("']) ");
    }

    private static boolean isExternalId(String taxonName) {
        return StringUtils.contains(taxonName, ":");
    }

    protected static List<String> collectParamValues(Map parameterMap, ParamName name) {
        List<String> taxa = new ArrayList<String>();
        if (parameterMap != null && parameterMap.containsKey(name.getName())) {
            Object paramObject = parameterMap.get(name.getName());
            if (paramObject instanceof String[]) {
                Collections.addAll(taxa, (String[]) paramObject);
            } else if (paramObject instanceof String) {
                taxa.add((String) paramObject);
            }
        }
        return taxa.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }

    public static CypherQuery createPagedQuery(HttpServletRequest request, CypherQuery query) {
        return createPagedQuery(request, query, DEFAULT_LIMIT);
    }

    public static CypherQuery createPagedQuery(HttpServletRequest request, CypherQuery query, long defaultLimit) {
        long defaultValue = 0L;
        long offset = getValueOrDefault(request, "offset", defaultValue);
        if (offset == defaultValue) {
            offset = getValueOrDefault(request, "skip", defaultValue);
        }
        long limit = getValueOrDefault(request, "limit", defaultLimit);
        return new CypherQuery(query.getQuery() + " SKIP " + offset + " LIMIT " + limit, query.getParams());
    }

    private static long getValueOrDefault(HttpServletRequest request, String paramName, long defaultValue) {
        long offset = defaultValue;
        if (request != null) {
            String offsetValue = request.getParameter(paramName);
            if (org.apache.commons.lang3.StringUtils.isNotBlank(offsetValue)) {
                try {
                    offset = Long.parseLong(offsetValue);
                } catch (NumberFormatException ex) {
                    LOG.warn("malformed " + paramName + " found [" + offsetValue + "]", ex);
                }
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

        query.append("MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study")
                .append(", sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon");
        if (RequestHelper.isSpatialSearch(parameterMap)) {
            query.append(", sourceSpecimen-[:COLLECTED_AT]->loc");
        }
        query.append(" WHERE not(has(interact.");
        query.append(PropertyAndValueDictionary.INVERTED);
        query.append("))");

        Map<String, String> params = addSourceWhereClause(parameterMap, query);

        query.append(" RETURN count(distinct(study)) as `number of distinct studies`")
                .append(", count(interact) as `number of interactions`")
                .append(", count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`")
                .append(", count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`")
                .append(", count(distinct(study.source)) as `number of distinct study sources`")
                .append(", count(c." + SpecimenConstant.DATE_IN_UNIX_EPOCH + "?) as `number of interactions with timestamp`")
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
        String sourceWhereClause = StringUtils.isBlank(source) ? "" : " study.source = {source}";
        Map<String, String> params = StringUtils.isBlank(source) ? EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};

        if (StringUtils.isNotBlank(sourceWhereClause)) {
            query.append(" AND");
            query.append(sourceWhereClause);
        }
        return params;
    }

    public static CypherQuery stats(final String source) {
        HashMap<String, String[]> paramMap = new HashMap<String, String[]>();
        if (StringUtils.isNotBlank(source)) {
            paramMap.put("source", new String[]{source});
        }
        return spatialInfo(paramMap);
    }
}
