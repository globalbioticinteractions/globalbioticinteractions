package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.server.util.RequestHelper;
import org.eol.globi.server.util.ResultFields;
import org.eol.globi.util.InteractUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CypherQueryBuilder {
    private static final Log LOG = LogFactory.getLog(CypherQueryBuilder.class);

    public static final String INTERACTION_PREYS_ON = "preysOn";
    public static final String INTERACTION_PREYED_UPON_BY = "preyedUponBy";

    public static final String INTERACTION_PARASITE_OF = "parasiteOf";
    public static final String INTERACTION_HOST_OF = "hostOf";

    private static final String SOURCE_TAXON_HTTP_PARAM_NAME = "sourceTaxon";
    private static final String TARGET_TAXON_HTTP_PARAM_NAME = "targetTaxon";
    private static final List<String> INVERTED_INTERACTION_TYPES = Arrays.asList(INTERACTION_PREYED_UPON_BY, INTERACTION_HOST_OF);
    private static final List<String> NON_INVERTED_INTERACTION_TYPES = Arrays.asList(INTERACTION_PREYS_ON, INTERACTION_PARASITE_OF);
    private static final Map<String, String> INTERACTION_TYPE_MAP = new HashMap<String, String>() {
        {
            String preysOn = InteractType.ATE + "|" + InteractType.PREYS_UPON;
            put(INTERACTION_PREYS_ON, preysOn);
            put(INTERACTION_PREYED_UPON_BY, preysOn);
            put(INTERACTION_PARASITE_OF, InteractType.PARASITE_OF + "|" + InteractType.HAS_HOST);
            put(INTERACTION_HOST_OF, InteractType.PARASITE_OF + "|" + InteractType.HAS_HOST);
        }
    };


    static final Map<String, String> EMPTY_PARAMS = new HashMap<String, String>();

    public static void addLocationClausesIfNecessary(StringBuilder query, Map parameterMap) {
        if (parameterMap != null) {
            RequestHelper.appendSpatialClauses(parameterMap, query);
        }
    }

    public static String lucenePathQuery(List<String> taxonNames) {
        int count = 0;
        StringBuilder lucenePathQuery = new StringBuilder();
        for (String taxonName : taxonNames) {
            if (count > 0) {
                lucenePathQuery.append(" OR ");
            }
            lucenePathQuery.append("path:\\\"" + taxonName + "\\\"");
            count++;
        }
        return lucenePathQuery.toString();
    }

    public static CypherQuery interactionObservations(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) {
        Map<String, String> queryParams;
        StringBuilder query = new StringBuilder();

        boolean isInvertedInteraction = INVERTED_INTERACTION_TYPES.contains(interactionType);

        String predatorPrefix = isInvertedInteraction ? ResultFields.PREFIX_TARGET_SPECIMEN : ResultFields.PREFIX_SOURCE_SPECIMEN;
        String preyPrefix = isInvertedInteraction ? ResultFields.PREFIX_SOURCE_SPECIMEN : ResultFields.PREFIX_TARGET_SPECIMEN;

        if (NON_INVERTED_INTERACTION_TYPES.contains(interactionType)) {
            query.append("START ");
            appendTaxonSelectors(sourceTaxonName != null, targetTaxonName != null, query);
            query.append(" ")
                    .append(getObservationMatchClause(INTERACTION_TYPE_MAP.get(interactionType)));
            appendSpatialClauses(parameterMap, query);
            query.append(" RETURN ")
                    .append("sourceTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME)
                    .append(",'").append(interactionType).append("' as ").append(ResultFields.INTERACTION_TYPE)
                    .append(",targetTaxon.name as ").append(ResultFields.TARGET_TAXON_NAME).append(", ");
            queryParams = getParams(sourceTaxonName, targetTaxonName);
        } else if (isInvertedInteraction) {
            // note that "preyedUponBy" is interpreted as an inverted "preysOn" relationship
            query.append("START ");
            appendTaxonSelectors(targetTaxonName != null, sourceTaxonName != null, query);
            query.append(" ").append(getObservationMatchClause(INTERACTION_TYPE_MAP.get(interactionType)));
            appendSpatialClauses(parameterMap, query);
            query.append(" RETURN ")
                    .append("targetTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME)
                    .append(",'").append(interactionType).append("' as ").append(ResultFields.INTERACTION_TYPE)
                    .append(",sourceTaxon.name as ").append(ResultFields.TARGET_TAXON_NAME).append(", ");
            queryParams = getParams(targetTaxonName, sourceTaxonName);
        } else {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }
        appendReturnClause(predatorPrefix, preyPrefix, query, RequestHelper.isSpatialSearch(parameterMap));

        return new CypherQuery(query.toString(), queryParams);
    }

    private static String getObservationMatchClause(String interactionTypes) {
        return "MATCH (sourceTaxon)<-[:CLASSIFIED_AS]-(sourceSpecimen)-[:" + interactionTypes + "]->(targetSpecimen)-[:CLASSIFIED_AS]->(targetTaxon)," +
                "(sourceSpecimen)<-[collected_rel:COLLECTED]-(study)";
    }

    private static void appendReturnClause(String predatorPrefix, String preyPrefix, StringBuilder returnClause, boolean spatialQuery) {
        if (spatialQuery) {
            returnClause.append("loc.").append(Location.LATITUDE).append("? as ").append(ResultFields.LATITUDE)
                    .append(",loc.").append(Location.LONGITUDE).append("? as ").append(ResultFields.LONGITUDE)
                    .append(",loc.").append(Location.ALTITUDE).append("? as ").append(ResultFields.ALTITUDE);
        } else {
            returnClause.append("null as ").append(ResultFields.LATITUDE)
                    .append(", null as ").append(ResultFields.LONGITUDE)
                    .append(", null as ").append(ResultFields.ALTITUDE);
        }

        returnClause.append(",study.").append(Study.TITLE).append(" as ").append(ResultFields.STUDY_TITLE)
                .append(",collected_rel.dateInUnixEpoch? as ").append(ResultFields.COLLECTION_TIME_IN_UNIX_EPOCH)
                .append(",ID(sourceSpecimen) as tmp_and_unique_")
                .append(predatorPrefix).append("_id,")
                .append("ID(targetSpecimen) as tmp_and_unique_")
                .append(preyPrefix).append("_id,")
                .append("sourceSpecimen.").append(Specimen.LIFE_STAGE_LABEL).append("? as ").append(predatorPrefix).append(ResultFields.SUFFIX_LIFE_STAGE).append(",")
                .append("targetSpecimen.").append(Specimen.LIFE_STAGE_LABEL).append("? as ").append(preyPrefix).append(ResultFields.SUFFIX_LIFE_STAGE).append(",")
                .append("sourceSpecimen.").append(Specimen.BODY_PART_LABEL).append("? as ").append(predatorPrefix).append(ResultFields.SUFFIX_BODY_PART).append(",")
                .append("targetSpecimen.").append(Specimen.BODY_PART_LABEL).append("? as ").append(preyPrefix).append(ResultFields.SUFFIX_BODY_PART).append(",")
                .append("sourceSpecimen.").append(Specimen.PHYSIOLOGICAL_STATE_LABEL).append("? as ").append(predatorPrefix).append(ResultFields.SUFFIX_PHYSIOLOGICAL_STATE).append(",")
                .append("targetSpecimen.").append(Specimen.PHYSIOLOGICAL_STATE_LABEL).append("? as ").append(preyPrefix).append(ResultFields.SUFFIX_PHYSIOLOGICAL_STATE).append(",")
                .append("targetSpecimen.").append(Specimen.TOTAL_COUNT).append("? as ").append(preyPrefix).append("_total_count").append(",")
                .append("targetSpecimen.").append(Specimen.TOTAL_VOLUME_IN_ML).append("? as ").append(preyPrefix).append("_total_volume_ml").append(",")
                .append("targetSpecimen.").append(Specimen.FREQUENCY_OF_OCCURRENCE).append("? as ").append(preyPrefix).append("_frequency_of_occurrence");
    }

    private static Map<String, String> getParams(final String sourceTaxonName, final String targetTaxonName) {
        return getParams(new ArrayList<String>() {{
                             if (sourceTaxonName != null) {
                                 add(sourceTaxonName);
                             }
                         }}, new ArrayList<String>() {{
                             if (targetTaxonName != null) {
                                 add(targetTaxonName);
                             }
                         }}
        );
    }

    private static Map<String, String> getParams(List<String> sourceTaxonNames, List<String> targetTaxonNames) {
        Map<String, String> paramMap = new HashMap<String, String>();
        if (sourceTaxonNames != null && sourceTaxonNames.size() > 0) {
            paramMap.put(ResultFields.SOURCE_TAXON_NAME, lucenePathQuery(sourceTaxonNames));
        }

        if (targetTaxonNames != null && targetTaxonNames.size() > 0) {
            paramMap.put(ResultFields.TARGET_TAXON_NAME, lucenePathQuery(targetTaxonNames));
        }
        return paramMap;
    }

    static void appendTaxonSelectors(boolean includeSourceTaxon, boolean includeTargetTaxon, StringBuilder query) {
        if (includeSourceTaxon) {
            final String sourceTaxonSelector = "sourceTaxon = " + getTaxonPathSelector(ResultFields.SOURCE_TAXON_NAME);
            query.append(sourceTaxonSelector);
        }
        if (includeTargetTaxon) {
            if (includeSourceTaxon) {
                query.append(", ");
            }
            final String targetTaxonSelector = "targetTaxon = " + getTaxonPathSelector(ResultFields.TARGET_TAXON_NAME);
            query.append(targetTaxonSelector);
        }
    }

    private static String getTaxonPathSelector(String taxonParamName) {
        return "node:taxonPaths({" + taxonParamName + "})";
    }

    private static void appendSpatialClauses(Map parameterMap, StringBuilder query) {
        if (parameterMap != null) {
            RequestHelper.appendSpatialClauses(parameterMap, query);
        }
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
        String whereClause = StringUtils.isBlank(source) ? "" : " WHERE study.source = {source}";
        Map<String, String> params = StringUtils.isBlank(source) ? EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};
        String cypherQuery = "START study=node:studies('*:*')" +
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:" + InteractUtil.allInteractionsCypherClause() + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                whereClause +
                " RETURN study.institution?, study.period?, study.description, study.contributor?, count(interact), count(distinct(sourceTaxon.name)), count(distinct(targetTaxon.name)), study.title, study.citation?, study.doi?, study.source";

        return new CypherQuery(cypherQuery, params);
    }

    public static CypherQuery locations() {
        String query = "START loc = node:locations('*:*') RETURN loc.latitude, loc.longitude";
        return new CypherQuery(query);
    }

    public static CypherQuery distinctInteractions(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) {
        StringBuilder query = new StringBuilder();
        Map<String, String> params;

        String interactionMatch = getInteractionMatch(INTERACTION_TYPE_MAP.get(interactionType));
        if (NON_INVERTED_INTERACTION_TYPES.contains(interactionType)) {
            query.append("START ");
            appendTaxonSelectors(sourceTaxonName != null, targetTaxonName != null, query);
            query.append(" ")
                    .append(interactionMatch);
            addLocationClausesIfNecessary(query, parameterMap);
            query.append(" RETURN sourceTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME).append(", '").append(interactionType).append("' as ").append(ResultFields.INTERACTION_TYPE).append(", collect(distinct(targetTaxon.name)) as ").append(ResultFields.TARGET_TAXON_NAME);
            params = getParams(sourceTaxonName, targetTaxonName);
        } else if (INVERTED_INTERACTION_TYPES.contains(interactionType)) {
            // "preyedUponBy is inverted interaction of "preysOn"
            query.append("START ");
            appendTaxonSelectors(targetTaxonName != null, sourceTaxonName != null, query);
            query.append(" ")
                    .append(interactionMatch);
            addLocationClausesIfNecessary(query, parameterMap);
            query.append(" RETURN targetTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME).append(", '").append(interactionType).append("' as ").append(ResultFields.INTERACTION_TYPE).append(", collect(distinct(sourceTaxon.name)) as ").append(ResultFields.TARGET_TAXON_NAME);
            params = getParams(targetTaxonName, sourceTaxonName);
        } else {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }

        return new CypherQuery(query.toString(), params);
    }

    private static String getInteractionMatch(String interactionTypeSelector) {
        return "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[:" + interactionTypeSelector + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon";
    }

    public static CypherQuery sourcesQuery() {
        String cypherQuery = "START study=node:studies('*:*')" +
                " RETURN distinct(study.source)";
        return new CypherQuery(cypherQuery, EMPTY_PARAMS);
    }

    public static CypherQuery buildInteractionQuery(Map parameterMap) {
        boolean spatialSearch = RequestHelper.isSpatialSearch(parameterMap);
        List<String> sourceTaxaSelectors = collectTaxa(parameterMap, SOURCE_TAXON_HTTP_PARAM_NAME);
        List<String> targetTaxaSelectors = collectTaxa(parameterMap, TARGET_TAXON_HTTP_PARAM_NAME);
        if (noSearchCriteria(spatialSearch, sourceTaxaSelectors, targetTaxaSelectors)) {
            // sensible default
            sourceTaxaSelectors.add("Homo sapiens");
        }

        StringBuilder query = new StringBuilder();
        query.append("START");
        if (spatialSearch) {
            query.append(" loc = node:locations('*:*')");
        } else {
            if (sourceTaxaSelectors.size() > 0) {
                query.append(" ");
                appendTaxonSelectors(true, false, query);
            } else if (targetTaxaSelectors.size() > 0) {
                query.append(" ");
                appendTaxonSelectors(false, true, query);
            }
        }
        query.append(" MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:")
                .append(InteractUtil.allInteractionsCypherClause())
                .append("]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon");

        if (parameterMap != null) {
            RequestHelper.appendSpatialClauses(parameterMap, query);
        }

        boolean hasWhereClause = false;
        if (sourceTaxaSelectors.size() > 0) {
            if (spatialSearch) {
                hasWhereClause = appendTaxonFilter(query, spatialSearch, "sourceTaxon", sourceTaxaSelectors);
            }
            appendTaxonFilter(query, hasWhereClause, "targetTaxon", targetTaxaSelectors);
        }

        query.append(" RETURN sourceTaxon.externalId? as ").append(ResultFields.SOURCE_TAXON_EXTERNAL_ID)
                .append(",sourceTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME)
                .append(",sourceTaxon.path? as ").append(ResultFields.SOURCE_TAXON_PATH)
                .append(",type(interactionType) as ").append(ResultFields.INTERACTION_TYPE)
                .append(",targetTaxon.externalId? as ").append(ResultFields.TARGET_TAXON_EXTERNAL_ID)
                .append(",targetTaxon.name as ").append(ResultFields.TARGET_TAXON_NAME)
                .append(",targetTaxon.path? as ").append(ResultFields.TARGET_TAXON_PATH);

        Map<String, String> params = null;
        if (!spatialSearch) {
            params = getParams(sourceTaxaSelectors, targetTaxaSelectors);
        }
        return new CypherQuery(query.toString(), params);
    }

    private static boolean noSearchCriteria(boolean spatialSearch, List<String> sourceTaxaSelectors, List<String> targetTaxaSelectors) {
        return !spatialSearch && sourceTaxaSelectors.size() == 0 && targetTaxaSelectors.size() == 0;
    }

    private static boolean appendTaxonFilter(StringBuilder query, boolean hasWhereClause, String taxonLabel, List<String> taxonNames) {
        if (taxonNames.size() > 0) {
            if (hasWhereClause) {
                query.append(" AND ");
            } else {
                query.append(" WHERE ");
                hasWhereClause = true;
            }
            query.append("has(").append(taxonLabel).append(".path) AND ").append(taxonLabel).append(".path =~ '(.*(");
            query.append(StringUtils.join(taxonNames, "|"));
            query.append(").*)'");
        }
        return hasWhereClause;
    }

    private static List<String> collectTaxa(Map parameterMap, String taxonSearchKey) {
        List<String> taxa = new ArrayList<String>();
        if (parameterMap.containsKey(taxonSearchKey)) {
            Object paramObject = parameterMap.get(taxonSearchKey);
            if (paramObject instanceof String[]) {
                for (String elem : (String[]) paramObject) {
                    taxa.add(elem);
                }
            } else if (paramObject instanceof String) {
                taxa.add((String) paramObject);
            }
        }
        return taxa;
    }


    public static CypherQuery createPagedQuery(HttpServletRequest request, CypherQuery query) {
        long offset = getValueOrDefault(request, "offset", 0L);
        long limit = getValueOrDefault(request, "limit", 1024L);
        return new CypherQuery(query.getQuery() + " SKIP " + offset + " LIMIT " + limit, query.getParams());
    }

    private static long getValueOrDefault(HttpServletRequest request, String paramName, long defaultValue) {
        long offset = defaultValue;
        if (request != null) {
            String offsetValue = request.getParameter(paramName);
            if (org.apache.commons.lang.StringUtils.isNotBlank(offsetValue)) {
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
            query.append("START loc = node:locations('*:*') WHERE");
            RequestHelper.addSpatialWhereClause(RequestHelper.parseSpatialSearchParams(parameterMap), query);
            query.append(" WITH loc");
        } else {
            query.append("START study = node:studies('*:*')");
        }

        query.append(" MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen<-[c:COLLECTED]-study")
                .append(", sourceSpecimen-[interact]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon");
        addLocationClausesIfNecessary(query, parameterMap);

        Map<String, String> cypherParams = addSourceClauseIfNecessary(query, parameterMap);

        query.append(" RETURN count(distinct(study)) as `number of distinct studies`")
                .append(", count(interact) as `number of interactions`")
                .append(", count(distinct(sourceTaxon.name)) as `number of distinct source taxa (e.g. predators)`")
                .append(", count(distinct(targetTaxon.name)) as `number of distinct target taxa (e.g. prey)`")
                .append(", count(distinct(study.source)) as `number of distinct study sources`")
                .append(", count(c." + Specimen.DATE_IN_UNIX_EPOCH + "?) as `number of interactions with timestamp`")
        ;
        if (RequestHelper.isSpatialSearch(parameterMap)) {
            query.append(", count(distinct(loc))");
        } else {
            query.append(", NULL");
        }
        query.append(" as `number of distinct locations`")
                .append(", count(distinct(").append(interactionLabel).append(")) as `number of distinct interactions`");
        return new CypherQuery(query.toString(), cypherParams);
    }

    private static Map<String, String> addSourceClauseIfNecessary(StringBuilder query, Map<String, String[]> parameterMap) {
        String[] sourceList = parameterMap == null ? null : parameterMap.get("source");
        final String source = sourceList != null && sourceList.length > 0 ? sourceList[0] : null;
        String sourceWhereClause = StringUtils.isBlank(source) ? "" : " study.source = {source}";
        Map<String, String> params = StringUtils.isBlank(source) ? EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};

        if (StringUtils.isNotBlank(sourceWhereClause)) {
            if (RequestHelper.isSpatialSearch(parameterMap)) {
                query.append(" AND");
            } else {
                query.append(" WHERE");
            }
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
