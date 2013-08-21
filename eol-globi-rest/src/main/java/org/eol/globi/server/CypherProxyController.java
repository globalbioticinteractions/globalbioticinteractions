package org.eol.globi.server;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.InteractUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CypherProxyController {

    public static final String OBSERVATION_MATCH =
            "MATCH (sourceTaxon)<-[:CLASSIFIED_AS]-(sourceSpecimen)-[:ATE]->(targetSpecimen)-[:CLASSIFIED_AS]->(targetTaxon)," +
                    "(sourceSpecimen)-[:COLLECTED_AT]->(loc)," +
                    "(sourceSpecimen)<-[collected_rel:COLLECTED]-(study) ";

    public static final String INTERACTION_PREYS_ON = "preysOn";
    public static final String INTERACTION_PREYED_UPON_BY = "preyedUponBy";

    public static final String INTERACTION_MATCH = "MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[:ATE]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon ";
    public static final String JSON_CYPHER_WRAPPER_PREFIX = "{\"query\":\"";

    private static final Map<String, String> EMPTY_PARAMS = new HashMap<String, String>();
    public static final String SOURCE_TAXON_HTTP_PARAM_NAME = "sourceTaxon";
    public static final String TARGET_TAXON_HTTP_PARAM_NAME = "targetTaxon";

    private void addLocationClausesIfNecessary(StringBuilder query, Map parameterMap) {
        query.append(" , sourceSpecimen-[:COLLECTED_AT]->loc ");
        query.append(parameterMap == null ? "" : RequestHelper.buildCypherSpatialWhereClause(parameterMap));
    }

    @RequestMapping(value = "/interactionTypes", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String getInteractionTypes() throws IOException {
        return "[ \"" + INTERACTION_PREYS_ON + "\",\"" + INTERACTION_PREYED_UPON_BY + "\"]";
    }

    @RequestMapping(value = "/interaction", method = RequestMethod.GET)
    @ResponseBody
    public String findInteractions(HttpServletRequest request) throws IOException {
        StringBuilder query = new StringBuilder();
        query.append("START loc = node:locations('*:*') ");
        addTaxonStartClausesIfNecessary(query, request.getParameterMap());

        query.append(" MATCH sourceTaxon<-[:CLASSIFIED_AS]-sourceSpecimen-[interactionType:")
                .append(InteractUtil.allInteractionsCypherClause())
                .append("]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon ");
        addLocationClausesIfNecessary(query, request.getParameterMap());

        query.append("RETURN sourceTaxon.externalId as ").append(ResultFields.SOURCE_TAXON_EXTERNAL_ID)
                .append(",sourceTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME)
                .append(",sourceTaxon.path as ").append(ResultFields.SOURCE_TAXON_PATH)
                .append(",type(interactionType) as ").append(ResultFields.INTERACTION_TYPE)
                .append(",targetTaxon.externalId as ").append(ResultFields.TARGET_TAXON_EXTERNAL_ID)
                .append(",targetTaxon.name as ").append(ResultFields.TARGET_TAXON_NAME)
                .append(",targetTaxon.path as ").append(ResultFields.TARGET_TAXON_PATH);
        return new CypherQueryExecutor(query.toString(), null).execute(request);
    }

    private void addTaxonStartClausesIfNecessary(StringBuilder query, Map parameterMap) {
        if (parameterMap.containsKey(SOURCE_TAXON_HTTP_PARAM_NAME)) {
            String luceneQuery = buildLuceneQuery(parameterMap.get(SOURCE_TAXON_HTTP_PARAM_NAME));
            query.append(", sourceTaxon = node:taxonpaths(\'" + luceneQuery + "\')");
        }
        if (parameterMap.containsKey(TARGET_TAXON_HTTP_PARAM_NAME)) {
            String luceneQuery = buildLuceneQuery(parameterMap.get(TARGET_TAXON_HTTP_PARAM_NAME));
            query.append(", targetTaxon = node:taxonpaths(\'" + luceneQuery + "\')");
        }
    }

    private String buildLuceneQuery(Object paramObject) {
        List<String> taxonNames = new ArrayList<String>();
        if (paramObject instanceof String[]) {
            String[] names = (String[]) paramObject;
            for (String name : names) {
                taxonNames.add("path:(" + name + ")");
            }
        } else if (paramObject instanceof String) {
            taxonNames.add("path:(" + paramObject + ")");
        }

        return StringUtils.join(taxonNames, " OR ");
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}", method = RequestMethod.GET, headers = "content-type=*/*")
    @ResponseBody
    public String findPreyOf(HttpServletRequest request,
                             @PathVariable("sourceTaxonName") String sourceTaxonName,
                             @PathVariable("interactionType") String interactionType) throws IOException {
        Map parameterMap = request == null ? null : request.getParameterMap();
        CypherQueryExecutor executor;
        if (shouldIncludeObservations(request, parameterMap)) {
            executor = findObservationsForInteraction(sourceTaxonName, interactionType, null, parameterMap);
        } else {
            executor = findDistinctTaxonInteractions(sourceTaxonName, interactionType, null, parameterMap);
        }

        return executor.execute(request);
    }


    public CypherQueryExecutor findDistinctTaxonInteractions(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) throws IOException {
        StringBuilder query = new StringBuilder();
        Map<String, String> params = EMPTY_PARAMS;
        if (INTERACTION_PREYS_ON.equals(interactionType)) {
            query.append("START ").append(getTaxonSelector(sourceTaxonName, targetTaxonName))
                    .append(" ")
                    .append(INTERACTION_MATCH);
            addLocationClausesIfNecessary(query, parameterMap);
            query.append("RETURN sourceTaxon.name as " + ResultFields.SOURCE_TAXON_NAME + ", '" + interactionType + "' as " + ResultFields.INTERACTION_TYPE + ", collect(distinct(targetTaxon.name)) as " + ResultFields.TARGET_TAXON_NAME);
            params = getParams(sourceTaxonName, targetTaxonName);
        } else if (INTERACTION_PREYED_UPON_BY.equals(interactionType)) {
            // "preyedUponBy is inverted interaction of "preysOn"
            query.append("START ").append(getTaxonSelector(targetTaxonName, sourceTaxonName))
                    .append(" ")
                    .append(INTERACTION_MATCH);
            addLocationClausesIfNecessary(query, parameterMap);
            query.append("RETURN targetTaxon.name as " + ResultFields.SOURCE_TAXON_NAME + ", '" + interactionType + "' as " + ResultFields.INTERACTION_TYPE + ", collect(distinct(sourceTaxon.name)) as " + ResultFields.TARGET_TAXON_NAME);
            params = getParams(targetTaxonName, sourceTaxonName);
        }

        if (query.length() == 0) {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }

        return new CypherQueryExecutor(query.toString(), params);
    }

    @RequestMapping(value = "/findTaxon/{taxonName}", method = RequestMethod.GET)
    @ResponseBody
    public String findTaxon(HttpServletRequest request, @PathVariable("taxonName") String taxonName) throws IOException {
        String query = "START taxon = node:taxons('*:*') " +
                "WHERE taxon.name =~ '" + taxonName + ".*'" +
                "RETURN distinct(taxon.name) " +
                "LIMIT 15";
        return new CypherQueryExecutor(query, null).execute(request);
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}/{targetTaxonName}", method = RequestMethod.GET)
    @ResponseBody
    public String findObservationsOf(HttpServletRequest request,
                                     @PathVariable("sourceTaxonName") String sourceTaxonName,
                                     @PathVariable("interactionType") String interactionType,
                                     @PathVariable("targetTaxonName") String targetTaxonName)
            throws IOException {
        CypherQueryExecutor result;
        Map parameterMap = request == null ? null : request.getParameterMap();

        if (shouldIncludeObservations(request, parameterMap)) {
            result = findObservationsForInteraction(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        } else {
            result = findDistinctTaxonInteractions(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        }

        return result.execute(request);
    }

    private boolean shouldIncludeObservations(HttpServletRequest request, Map parameterMap) {
        String includeObservations = parameterMap == null ? null : request.getParameter("includeObservations");
        return "true".equalsIgnoreCase(includeObservations);
    }

    private CypherQueryExecutor findObservationsForInteraction(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) throws IOException {
        Map<String, String> query_params = EMPTY_PARAMS;
        StringBuilder query = new StringBuilder();
        boolean isInvertedInteraction = INTERACTION_PREYED_UPON_BY.equals(interactionType);

        String predatorPrefix = isInvertedInteraction ? ResultFields.PREFIX_TARGET_SPECIMEN : ResultFields.PREFIX_SOURCE_SPECIMEN;
        String preyPrefix = isInvertedInteraction ? ResultFields.PREFIX_SOURCE_SPECIMEN : ResultFields.PREFIX_TARGET_SPECIMEN;

        final StringBuilder returnClause = new StringBuilder();
        returnClause.append("loc.latitude as ").append(ResultFields.LATITUDE)
                .append(",loc.longitude as ").append(ResultFields.LONGITUDE)
                .append(",loc.altitude? as ").append(ResultFields.ALTITUDE)
                .append(",study.title as ").append(ResultFields.STUDY_TITLE)
                .append(",collected_rel.dateInUnixEpoch? as ").append(ResultFields.COLLECTION_TIME_IN_UNIX_EPOCH)
                .append(",ID(sourceSpecimen) as tmp_and_unique_")
                .append(predatorPrefix).append("_id,")
                .append("ID(targetSpecimen) as tmp_and_unique_")
                .append(preyPrefix).append("_id,")
                .append("sourceSpecimen.lifeStage? as ").append(predatorPrefix).append(ResultFields.SUFFIX_LIFE_STAGE).append(",")
                .append("targetSpecimen.lifeStage? as ").append(preyPrefix).append(ResultFields.SUFFIX_LIFE_STAGE).append(",")
                .append("sourceSpecimen.bodyPart? as ").append(predatorPrefix).append(ResultFields.SUFFIX_BODY_PART).append(",")
                .append("targetSpecimen.bodyPart? as ").append(preyPrefix).append(ResultFields.SUFFIX_BODY_PART).append(",")
                .append("sourceSpecimen.physiologicalState? as ").append(predatorPrefix).append(ResultFields.SUFFIX_PHYSIOLOGICAL_STATE).append(",")
                .append("targetSpecimen.physiologicalState? as ").append(preyPrefix).append(ResultFields.SUFFIX_PHYSIOLOGICAL_STATE);


        if (INTERACTION_PREYS_ON.equals(interactionType)) {
            query.append("START ").append(getTaxonSelector(sourceTaxonName, targetTaxonName)).append(" ")
                    .append(OBSERVATION_MATCH)
                    .append(getSpatialWhereClause(parameterMap))
                    .append(" RETURN ")
                    .append("sourceTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME)
                    .append(",'").append(interactionType).append("' as ").append(ResultFields.INTERACTION_TYPE)
                    .append(",targetTaxon.name as ").append(ResultFields.TARGET_TAXON_NAME).append(", ")
                    .append(returnClause);
            query_params = getParams(sourceTaxonName, targetTaxonName);
        } else if (isInvertedInteraction) {
            // note that "preyedUponBy" is interpreted as an inverted "preysOn" relationship
            query.append("START ").append(getTaxonSelector(targetTaxonName, sourceTaxonName)).append(" ")
                    .append(OBSERVATION_MATCH)
                    .append(getSpatialWhereClause(parameterMap))
                    .append(" RETURN ")
                    .append("targetTaxon.name as ").append(ResultFields.SOURCE_TAXON_NAME)
                    .append(",'").append(interactionType).append("' as ").append(ResultFields.INTERACTION_TYPE)
                    .append(",sourceTaxon.name as ").append(ResultFields.TARGET_TAXON_NAME).append(", ")
                    .append(returnClause);
            query_params = getParams(targetTaxonName, sourceTaxonName);
        } else {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }

        return new CypherQueryExecutor(query.toString(), query_params);
    }

    private Map<String, String> getParams(String sourceTaxonName, String targetTaxonName) {
        Map<String, String> paramMap = new HashMap<String, String>();
        if (sourceTaxonName != null) {
            paramMap.put(ResultFields.SOURCE_TAXON_NAME, sourceTaxonName);
        }

        if (targetTaxonName != null) {
            paramMap.put(ResultFields.TARGET_TAXON_NAME, targetTaxonName);
        }
        return paramMap;
    }

    private String getTaxonSelector(String sourceTaxonName, String targetTaxonName) {
        final String sourceTaxonSelector = "sourceTaxon = node:taxons(name={" + ResultFields.SOURCE_TAXON_NAME + "})";
        final String targetTaxonSelector = "targetTaxon = node:taxons(name={" + ResultFields.TARGET_TAXON_NAME + "})";
        StringBuilder builder = new StringBuilder();
        if (sourceTaxonName != null) {
            builder.append(sourceTaxonSelector);
        }
        if (targetTaxonName != null) {
            if (sourceTaxonName != null) {
                builder.append(", ");
            }
            builder.append(targetTaxonSelector);
        }

        return builder.toString();
    }


    public String findObservationsOf(HttpServletRequest request,
                                     String sourceTaxonName,
                                     String interactionType) throws IOException {
        return findObservationsOf(request, sourceTaxonName, interactionType, null);
    }


    private String getSpatialWhereClause(Map parameterMap) {
        return parameterMap == null ? "" : RequestHelper.buildCypherSpatialWhereClause(parameterMap);
    }

    @RequestMapping(value = "/locations", method = RequestMethod.GET)
    @ResponseBody
    @Cacheable(value = "locationCache")
    public String locations(HttpServletRequest request) throws IOException {
        return new CypherQueryExecutor("START loc = node:locations('*:*') RETURN loc.latitude, loc.longitude", null).execute(request);
    }


    @RequestMapping(value = "/contributors", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Cacheable(value = "contributorCache")
    public String contributors() throws IOException {
        String cypherQuery = "START study=node:studies('*:*')" +
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:" + InteractUtil.allInteractionsCypherClause() + "]->targetSpecimen-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                " RETURN study.institution?, study.period?, study.description, study.contributor, count(interact), count(distinct(sourceTaxon)), count(distinct(targetTaxon))";
        return new CypherQueryExecutor(cypherQuery, EMPTY_PARAMS).execute(null);
    }


    @RequestMapping(value = "/findExternalUrlForTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findExternalLinkForTaxonWithName(HttpServletRequest request, @PathVariable("taxonName") String taxonName) throws IOException {
        String result = findExternalIdForTaxon(request, taxonName);

        String url = null;
        for (Map.Entry<String, String> stringStringEntry : ExternalIdUtil.getURLPrefixMap().entrySet()) {
            url = getUrl(result, stringStringEntry.getKey(), stringStringEntry.getValue());
            if (url != null) {
                break;
            }

        }
        return buildJsonUrl(url);
    }

    @RequestMapping(value = "/findExternalIdForTaxon/{taxonName}", method = RequestMethod.GET)
    @ResponseBody
    public String findExternalIdForTaxon(HttpServletRequest request, @PathVariable("taxonName") final String taxonName) throws IOException {
        String query = "START taxon = node:taxons(name={taxonName}) " +
                " RETURN taxon.externalId as externalId";

        return new CypherQueryExecutor(query, new HashMap<String, String>() {{
            put("taxonName", taxonName);
        }}).execute(request);
    }

    private String buildJsonUrl(String url) {
        return url == null ? "{}" : "{\"url\":\"" + url + "\"}";
    }

    @RequestMapping(value = "/findExternalUrlForExternalId/{externalId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findExternalLinkForExternalId(@PathVariable("externalId") String externalId) {
        return buildJsonUrl(ExternalIdUtil.infoURLForExternalId(externalId));
    }

    @RequestMapping(value = "/shortestPathsBetweenTaxon/{startTaxon}/andTaxon/{endTaxon}", method = RequestMethod.GET)
    @ResponseBody
    public String findShortestPaths(HttpServletRequest request, @PathVariable("startTaxon") final String startTaxon,
                                    @PathVariable("endTaxon") final String endTaxon) throws IOException {
        String query = "START startNode = node:taxons(name={startTaxon}),endNode = node:taxons(name={endTaxon}) " +
                "MATCH p = allShortestPaths(startNode-[:" + InteractUtil.allInteractionsCypherClause() + "|CLASSIFIED_AS*..100]-endNode) " +
                "RETURN extract(n in (filter(x in nodes(p) : has(x.name))) : " +
                "coalesce(n.name?)) as shortestPaths " +
                "LIMIT 10";

        return new CypherQueryExecutor(query, new HashMap<String, String>() {{
            put("startTaxon", startTaxon);
            put("endTaxon", endTaxon);
        }}).execute(request);
    }

    private String getUrl(String result, String externalIdPrefix, String urlPrefix) {
        String url = "";
        if (result.contains(externalIdPrefix)) {
            String[] split = result.split(externalIdPrefix);
            if (split.length > 1) {
                String[] externalIdParts = split[1].split("\"");
                if (externalIdParts.length > 1) {
                    url = urlPrefix + externalIdParts[0];
                }
            }
        }
        return url;
    }


}