package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.InteractUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CypherQueryBuilder {
    public static final String DEFAULT_LIMIT_CLAUSE = "LIMIT 512";
    public static final String SOURCE_TAXON_HTTP_PARAM_NAME = "sourceTaxon";
    public static final String TARGET_TAXON_HTTP_PARAM_NAME = "targetTaxon";
    public static final String OBSERVATION_MATCH =
            "MATCH (sourceTaxon)<-[:CLASSIFIED_AS]-(sourceSpecimen)-[:" + preysOn() + "]->(targetSpecimen)-[:CLASSIFIED_AS]->(targetTaxon)," +
                    "(sourceSpecimen)<-[collected_rel:COLLECTED]-(study)";
    public static final String INTERACTION_MATCH = "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[:" + preysOn() + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon";

    private static String preysOn() {
        return InteractType.ATE + "|" + InteractType.PREYS_UPON;
    }

    public static final String INTERACTION_PREYS_ON = "preysOn";
    public static final String INTERACTION_PREYED_UPON_BY = "preyedUponBy";
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
        boolean isInvertedInteraction = INTERACTION_PREYED_UPON_BY.equals(interactionType);

        String predatorPrefix = isInvertedInteraction ? ResultFields.PREFIX_TARGET_SPECIMEN : ResultFields.PREFIX_SOURCE_SPECIMEN;
        String preyPrefix = isInvertedInteraction ? ResultFields.PREFIX_SOURCE_SPECIMEN : ResultFields.PREFIX_TARGET_SPECIMEN;

        if (INTERACTION_PREYS_ON.equals(interactionType)) {
            query.append("START ");
            appendTaxonSelectors(sourceTaxonName != null, targetTaxonName != null, query);
            query.append(" ")
                    .append(OBSERVATION_MATCH);
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
            query.append(" ").append(OBSERVATION_MATCH);
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
        return "node:taxonpaths({" + taxonParamName + "})";
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
                "coalesce(n.name?)) as shortestPaths " +
                "LIMIT 10";


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

    public static CypherQuery stats(final String source) {
        String whereClause = StringUtils.isBlank(source) ? "" : " WHERE study.source = {source}";
        Map<String, String> params = StringUtils.isBlank(source) ? EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};
        String cypherQuery = "START study=node:studies('*:*')" +
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:" + InteractUtil.allInteractionsCypherClause() + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                whereClause +
                " RETURN count(distinct(study)) as `number of studies`, count(interact) as `number of interactions`, count(distinct(sourceTaxon)) as `number of distinct source taxa (e.g. predators)`, count(distinct(targetTaxon)) as `number of distinct target taxa (e.g. prey)`, count(distinct(study.source)) as `number of distinct study sources`";

        return new CypherQuery(cypherQuery, params);
    }

    public static CypherQuery references(final String source) {
        String whereClause = StringUtils.isBlank(source) ? "" : " WHERE study.source = {source}";
        Map<String, String> params = StringUtils.isBlank(source) ? EMPTY_PARAMS : new HashMap<String, String>() {{
            put("source", source);
        }};
        String cypherQuery = "START study=node:studies('*:*')" +
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:" + InteractUtil.allInteractionsCypherClause() + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                whereClause +
                " RETURN study.institution?, study.period?, study.description, study.contributor?, count(interact), count(distinct(sourceTaxon)), count(distinct(targetTaxon)), study.title, study.citation?, study.doi?, study.source";

        return new CypherQuery(cypherQuery, params);
    }

    public static CypherQuery locations() {
        String query = "START loc = node:locations('*:*') RETURN loc.latitude, loc.longitude";
        return new CypherQuery(query);
    }

    public static CypherQuery findTaxon(String taxonName) {
        String query = "START taxon = node:taxons('*:*') " +
                "WHERE taxon.name =~ '" + taxonName + ".*'" +
                "RETURN distinct(taxon.name) " +
                "LIMIT 15";
        return new CypherQuery(query);
    }

    public static CypherQuery distinctInteractions(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) {
        StringBuilder query = new StringBuilder();
        Map<String, String> params = EMPTY_PARAMS;
        if (INTERACTION_PREYS_ON.equals(interactionType)) {
            query.append("START ");
            appendTaxonSelectors(sourceTaxonName != null, targetTaxonName != null, query);
            query.append(" ")
                    .append(INTERACTION_MATCH);
            addLocationClausesIfNecessary(query, parameterMap);
            query.append(" RETURN sourceTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME).append(", '").append(interactionType).append("' as ").append(ResultFields.INTERACTION_TYPE).append(", collect(distinct(targetTaxon.name)) as ").append(ResultFields.TARGET_TAXON_NAME);
            params = getParams(sourceTaxonName, targetTaxonName);
        } else if (INTERACTION_PREYED_UPON_BY.equals(interactionType)) {
            // "preyedUponBy is inverted interaction of "preysOn"
            query.append("START ");
            appendTaxonSelectors(targetTaxonName != null, sourceTaxonName != null, query);
            query.append(" ")
                    .append(INTERACTION_MATCH);
            addLocationClausesIfNecessary(query, parameterMap);
            query.append(" RETURN targetTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME).append(", '").append(interactionType).append("' as ").append(ResultFields.INTERACTION_TYPE).append(", collect(distinct(sourceTaxon.name)) as ").append(ResultFields.TARGET_TAXON_NAME);
            params = getParams(targetTaxonName, sourceTaxonName);
        }

        if (query.length() == 0) {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }

        return new CypherQuery(query.toString(), params);
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

        // fix quick before introducing smarter way to chunk the results
        query.append(" ");
        query.append(DEFAULT_LIMIT_CLAUSE);
        Map<String, String> params = null;
        if (!spatialSearch) {
            params = getParams(sourceTaxaSelectors, targetTaxaSelectors);
        }
        return new CypherQuery(query.toString(), params);
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


}
