package org.eol.globi.server;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eol.globi.util.ExternalIdUtil;
import org.eol.globi.util.InteractUtil;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Controller
public class CypherProxyController {

    @Autowired
    private EmbeddedGraphDatabase graphDb;

    public static final String OBSERVATION_MATCH =
            "MATCH (predatorTaxon)<-[:CLASSIFIED_AS]-(predator)-[:ATE]->(prey)-[:CLASSIFIED_AS]->(preyTaxon)," +
                    "(predator)-[:COLLECTED_AT]->(loc)," +
                    "(predator)<-[collected_rel:COLLECTED]-(study) ";

    public static final String INCLUDE_OBSERVATIONS_TRUE = "includeObservations=true";

    public static final String INTERACTION_PREYS_ON = "preysOn";
    public static final String INTERACTION_PREYED_UPON_BY = "preyedUponBy";
    public static final String DEFAULT_RETURN_LIST = "loc.latitude as latitude," +
            "loc.longitude as longitude," +
            "loc.altitude? as altitude," +
            "study.title," +
            "collected_rel.dateInUnixEpoch? as collection_time_in_unix_epoch," +
            "ID(predator) as tmp_and_unique_specimen_id," +
            "predator.lifeStage? as predator_life_stage," +
            "prey.lifeStage? as prey_life_stage," +
            "predator.bodyPart? as predator_body_part," +
            "prey.bodyPart? as prey_body_part," +
            "predator.physiologicalState? as predator_physiological_state," +
            "prey.physiologicalState? as prey_physiological_state";

    public static final String INTERACTION_MATCH = "MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[:ATE]->prey-[:CLASSIFIED_AS]->preyTaxon ";
    public static final String JSON_CYPHER_WRAPPER_PREFIX = "{\"query\":\"";

    private static final Map<String, String> EMPTY_PARAMS = new HashMap<String, String>();

    private void addLocationClausesIfNecessary(StringBuilder query, Map parameterMap) {
        query.append(" , predator-[:COLLECTED_AT]->loc ");
        query.append(parameterMap == null ? "" : RequestHelper.buildCypherSpatialWhereClause(parameterMap));
    }

    private Map<String, String> buildParams(String scientificName) {
        Map<String, String> params = new HashMap<String, String>();
        if (scientificName != null) {
            params.put("scientificName", scientificName);
        }

        return params;
    }

    @RequestMapping(value = "/interactionTypes", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String getInteractionTypes(HttpServletRequest request) throws IOException {
        return "[ \"" + INTERACTION_PREYS_ON + "\",\"" + INTERACTION_PREYED_UPON_BY + "\"]";
    }

    @RequestMapping(value = "/interaction", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findInteractions(HttpServletRequest request) throws IOException {
        StringBuilder query = new StringBuilder();
        query.append("START loc = node:locations('*:*') " +
                "MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[interactionType:" + InteractUtil.allInteractionsCypherClause() + "]->prey-[:CLASSIFIED_AS]->preyTaxon ");
        addLocationClausesIfNecessary(query, request.getParameterMap());
        query.append("RETURN predatorTaxon.externalId, predatorTaxon.name as predatorName, type(interactionType), preyTaxon.externalId, preyTaxon.name as preyTaxon");
        return new QueryExecutor(query.toString(), null).execute(request);
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}", method = RequestMethod.GET, produces = "application/json", headers = "content-type=*/*")
    @ResponseBody
    public String findPreyOf(HttpServletRequest request,
                             @PathVariable("sourceTaxonName") String sourceTaxonName,
                             @PathVariable("interactionType") String interactionType) throws IOException {
        Map parameterMap = request == null ? null : request.getParameterMap();
        return findDistinctTaxonInteractions(sourceTaxonName, interactionType, null, parameterMap).execute(request);
    }


    public QueryExecutor findDistinctTaxonInteractions(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) throws IOException {
        StringBuilder query = new StringBuilder();
        Map<String, String> params = EMPTY_PARAMS;
        if (INTERACTION_PREYS_ON.equals(interactionType)) {
            query.append("START ").append(getTaxonSelector(sourceTaxonName, targetTaxonName))
                    .append(" ")
                    .append(INTERACTION_MATCH);
            addLocationClausesIfNecessary(query, parameterMap);
            query.append("RETURN distinct(preyTaxon.name) as preyName");
            params = getParams(sourceTaxonName, targetTaxonName);
        } else if (INTERACTION_PREYED_UPON_BY.equals(interactionType)) {
            // "preyedUponBy is inverted interaction of "preysOn"
            query.append("START ").append(getTaxonSelector(targetTaxonName, sourceTaxonName))
                    .append(" ")
                    .append(INTERACTION_MATCH);
            addLocationClausesIfNecessary(query, parameterMap);
            query.append("RETURN distinct(predatorTaxon.name) as preyName");
            params = getParams(targetTaxonName, sourceTaxonName);
        }

        if (query.length() == 0) {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }

        return new QueryExecutor(query.toString(), params);
    }

    @RequestMapping(value = "/findTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findTaxon(HttpServletRequest request, @PathVariable("taxonName") String taxonName) throws IOException {
        String query = "START taxon = node:taxons('*:*') " +
                "WHERE taxon.name =~ '" + taxonName + ".*'" +
                "RETURN distinct(taxon.name) " +
                "LIMIT 15";
        return new QueryExecutor(query, null).execute(request);
    }

    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}/{targetTaxonName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findObservationsOf(HttpServletRequest request,
                                     @PathVariable("sourceTaxonName") String sourceTaxonName,
                                     @PathVariable("interactionType") String interactionType,
                                     @PathVariable("targetTaxonName") String targetTaxonName)
            throws IOException {
        Map parameterMap = request == null ? null : request.getParameterMap();

        String includeObservations = parameterMap == null ? null : request.getParameter("includeObservations");
        QueryExecutor result;
        if ("true".equalsIgnoreCase(includeObservations)) {
            result = findObservationsForInteraction(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        } else {
            result = findDistinctTaxonInteractions(sourceTaxonName, interactionType, targetTaxonName, parameterMap);
        }

        return result.execute(request);
    }

    private QueryExecutor findObservationsForInteraction(String sourceTaxonName, String interactionType, String targetTaxonName, Map parameterMap) throws IOException {
        Map<String, String> query_params = EMPTY_PARAMS;
        String query = null;

        if (INTERACTION_PREYS_ON.equals(interactionType)) {
            query = "START " + getTaxonSelector(sourceTaxonName, targetTaxonName) +
                    OBSERVATION_MATCH +
                    getSpatialWhereClause(parameterMap) +
                    " RETURN preyTaxon.name as preyName, " +
                    DEFAULT_RETURN_LIST;
            query_params = getParams(sourceTaxonName, targetTaxonName);
        } else if (INTERACTION_PREYED_UPON_BY.equals(interactionType)) {
            // note that "preyedUponBy" is interpreted as an inverted "preysOn" relationship
            query = "START " + getTaxonSelector(targetTaxonName, sourceTaxonName) +
                    OBSERVATION_MATCH +
                    getSpatialWhereClause(parameterMap) +
                    " RETURN predatorTaxon.name as predatorName, " +
                    DEFAULT_RETURN_LIST;
            query_params = getParams(targetTaxonName, sourceTaxonName);
        }
        if (query == null) {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }

        return new QueryExecutor(query, query_params);
    }

    private Map<String, String> getParams(String sourceTaxonName, String targetTaxonName) {
        Map<String, String> paramMap = new HashMap<String, String>();
        if (sourceTaxonName != null) {
            paramMap.put("predatorName", sourceTaxonName);
        }

        if (targetTaxonName != null) {
            paramMap.put("preyName", targetTaxonName);
        }
        return paramMap;
    }

    private String buildJSONParamList(Map<String, String> paramMap) {
        StringBuilder builder = new StringBuilder();
        if (paramMap != null) {
            populateParams(paramMap, builder);
        }
        return builder.toString();
    }

    private void populateParams(Map<String, String> paramMap, StringBuilder builder) {
        Iterator<Map.Entry<String, String>> iterator = paramMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> param = iterator.next();
            String jsonParam = "\"" + param.getKey() + "\" : \"" + param.getValue() + "\"";
            builder.append(jsonParam);
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
    }

    private String getTaxonSelector(String sourceTaxonName, String targetTaxonName) {
        final String sourceTaxonSelector = "predatorTaxon = node:taxons(name={predatorName})";
        final String targetTaxonSelector = "preyTaxon = node:taxons(name={preyName})";
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


    @RequestMapping(value = "/taxon/{sourceTaxonName}/{interactionType}", params = {INCLUDE_OBSERVATIONS_TRUE}, method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findObservationsOf(HttpServletRequest request,
                                     @PathVariable("sourceTaxonName") String sourceTaxonName,
                                     @PathVariable("interactionType") String interactionType) throws IOException {
        return findObservationsOf(request, sourceTaxonName, interactionType, null);
    }


    private String getSpatialWhereClause(Map parameterMap) {
        return parameterMap == null ? "" : RequestHelper.buildCypherSpatialWhereClause(parameterMap);
    }

    @RequestMapping(value = "/locations", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Cacheable(value = "locationCache")
    public String locations(HttpServletRequest request) throws IOException {
        return new QueryExecutor("START loc = node:locations('*:*') RETURN loc.latitude, loc.longitude", null).execute(request);
    }


    @RequestMapping(value = "/contributors", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Cacheable(value = "contributorCache")
    public String contributors(HttpServletRequest request) throws IOException {
        String cypherQuery = "START study=node:studies('*:*')" +
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:" + InteractUtil.allInteractionsCypherClause() + "]->prey-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                " RETURN study.institution, study.period, study.description, study.contributor, count(interact), count(distinct(sourceTaxon)), count(distinct(targetTaxon))";
        return new QueryExecutor(cypherQuery, EMPTY_PARAMS).execute(request);
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

    @RequestMapping(value = "/findExternalIdForTaxon/{taxonName}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String findExternalIdForTaxon(HttpServletRequest request, @PathVariable("taxonName") final String taxonName) throws IOException {
        String query = "START taxon = node:taxons(name={taxonName}) " +
                " RETURN taxon.externalId as externalId";

        return new QueryExecutor(query, new HashMap<String, String>() {{
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

    @RequestMapping(value = "/shortestPathsBetweenTaxon/{startTaxon}/andTaxon/{endTaxon}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public QueryExecutor findShortestPaths(@PathVariable("startTaxon") final String startTaxon, @PathVariable("endTaxon") final String endTaxon) throws IOException {
        String query = "START startNode = node:taxons(name={startTaxon}),endNode = node:taxons(name={endTaxon}) " +
                "MATCH p = allShortestPaths(startNode-[:" + InteractUtil.allInteractionsCypherClause() + "|CLASSIFIED_AS*..100]-endNode) " +
                "RETURN extract(n in (filter(x in nodes(p) : has(x.name))) : " +
                "coalesce(n.name?)) as shortestPaths " +
                "LIMIT 10";

        return new QueryExecutor(query, new HashMap<String, String>() {{
            put("startTaxon", startTaxon);
            put("endTaxon", endTaxon);
        }});
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


    public class QueryExecutor {
        private final String query;
        private final Map<String, String> params;

        public QueryExecutor(String query, Map<String, String> query_params) {
            this.query = query;
            this.params = query_params;
        }

        public String execute(HttpServletRequest request) throws IOException {
            String result;
            String type = request == null ? "json" : request.getParameter("type");
            if (type == null || "json".equalsIgnoreCase(type)) {
                result = executeRemote();
            } else if ("csv".equalsIgnoreCase(type)) {
                result = executeLocal();
            } else {
                throw new IOException("found unsupported return format type request for [" + type + "]");
            }
            return result;
        }

        private String executeRemote() throws IOException {
            String result;
            org.apache.http.client.HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://46.4.36.142:7474/db/data/cypher");
            HttpClient.addJsonHeaders(httpPost);
            httpPost.setEntity(new StringEntity(wrapQuery(query, params)));
            BasicResponseHandler responseHandler = new BasicResponseHandler();
            result = httpclient.execute(httpPost, responseHandler);
            return result;
        }

        private String executeLocal() {
            String result;ExecutionEngine engine = new ExecutionEngine(graphDb);
            Map<String, Object> map = new HashMap<String, Object>();
            map.putAll(params);
            ExecutionResult results = engine.execute(query, map);
            boolean wroteHeader = false;
            StringBuilder resultBuilder = new StringBuilder();
            ArrayList<String> header = new ArrayList<String>();
            for (Map<String, Object> resultEntry : results) {
                if (!wroteHeader) {
                    header.addAll(resultEntry.keySet());
                    writeHeader(resultBuilder, header);
                    wroteHeader = true;
                }

                Iterator<String> headerFields = header.iterator();
                while (headerFields.hasNext()) {
                    resultBuilder.append('\"');
                    resultBuilder.append(resultEntry.get(headerFields.next()));
                    resultBuilder.append('\"');
                    if (headerFields.hasNext()) {
                        resultBuilder.append(',');
                    }
                }
            }
            result = resultBuilder.toString();
            return result;
        }

        private void writeHeader(StringBuilder resultBuilder, ArrayList<String> header) {
            Iterator<String> iterator = header.iterator();
            while (iterator.hasNext()) {
                resultBuilder.append("\"");
                resultBuilder.append(iterator.next());
                resultBuilder.append("\"");
                if (iterator.hasNext()) {
                    resultBuilder.append(",");
                }

            }
        }

        private String wrapQuery(String cypherQuery, Map<String, String> params) {
            String query = JSON_CYPHER_WRAPPER_PREFIX;
            query += cypherQuery;
            query += " \", \"params\": {" + buildJSONParamList(params) + " } }";
            return query;
        }
    }
}