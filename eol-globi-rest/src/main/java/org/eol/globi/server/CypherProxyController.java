package org.eol.globi.server;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Controller
public class CypherProxyController {

    public static final String OBSERVATION_MATCH =
            "MATCH (predatorTaxon)<-[:CLASSIFIED_AS]-(predator)-[:ATE]->(prey)-[:CLASSIFIED_AS]->(preyTaxon)," +
                    "(predator)-[:COLLECTED_AT]->(loc)," +
                    "(predator)<-[collected_rel:COLLECTED]-(study) ";

    public static final String INTERACTION_PREYS_ON = "preysOn";
    public static final String INTERACTION_PREYED_UPON_BY = "preyedUponBy";

    public static final String INTERACTION_MATCH = "MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[:ATE]->prey-[:CLASSIFIED_AS]->preyTaxon ";
    public static final String JSON_CYPHER_WRAPPER_PREFIX = "{\"query\":\"";

    private static final Map<String, String> EMPTY_PARAMS = new HashMap<String, String>();

    private void addLocationClausesIfNecessary(StringBuilder query, Map parameterMap) {
        query.append(" , predator-[:COLLECTED_AT]->loc ");
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
        query.append("START loc = node:locations('*:*') ")
                .append("MATCH predatorTaxon<-[:CLASSIFIED_AS]-predator-[interactionType:")
                .append(InteractUtil.allInteractionsCypherClause())
                .append("]->prey-[:CLASSIFIED_AS]->preyTaxon ");
        addLocationClausesIfNecessary(query, request.getParameterMap());
        query.append("RETURN predatorTaxon.externalId, predatorTaxon.name as predatorName, type(interactionType), preyTaxon.externalId, preyTaxon.name as preyTaxon");
        return new CypherQueryExecutor(query.toString(), null).execute(request);
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
        String query = null;
        final String DEFAULT_RETURN_LIST = "loc.latitude as latitude," +
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


        if (INTERACTION_PREYS_ON.equals(interactionType)) {
            query = "START " + getTaxonSelector(sourceTaxonName, targetTaxonName) +
                    OBSERVATION_MATCH +
                    getSpatialWhereClause(parameterMap) +
                    " RETURN preyTaxon.name as preyName, " +
                    DEFAULT_RETURN_LIST +
                    ",predator.name as predatorName," +
                    "\"" + interactionType + "\" as interaction_type";
            query_params = getParams(sourceTaxonName, targetTaxonName);
        } else if (INTERACTION_PREYED_UPON_BY.equals(interactionType)) {
            // note that "preyedUponBy" is interpreted as an inverted "preysOn" relationship
            query = "START " + getTaxonSelector(targetTaxonName, sourceTaxonName) +
                    OBSERVATION_MATCH +
                    getSpatialWhereClause(parameterMap) +
                    " RETURN predatorTaxon.name as predatorName, " +
                    DEFAULT_RETURN_LIST +
                    ",preyName.name as preyName" +
                    ",\"" + interactionType + "\" as interaction_type"
            ;
            query_params = getParams(targetTaxonName, sourceTaxonName);
        }
        if (query == null) {
            throw new IllegalArgumentException("unsupported interaction type [" + interactionType + "]");
        }

        return new CypherQueryExecutor(query, query_params);
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
                " MATCH study-[:COLLECTED]->sourceSpecimen-[interact:" + InteractUtil.allInteractionsCypherClause() + "]->prey-[:CLASSIFIED_AS]->targetTaxon, sourceSpecimen-[:CLASSIFIED_AS]->sourceTaxon " +
                " RETURN study.institution, study.period, study.description, study.contributor, count(interact), count(distinct(sourceTaxon)), count(distinct(targetTaxon))";
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


    public class CypherQueryExecutor {
        private final String query;
        private final Map<String, String> params;

        public CypherQueryExecutor(String query, Map<String, String> query_params) {
            this.query = query;
            this.params = query_params;
        }

        public String execute(HttpServletRequest request) throws IOException {
            String result;
            String type = request == null ? "json" : request.getParameter("type");
            if (type == null || "json".equalsIgnoreCase(type)) {
                result = executeRemote();
            } else if ("csv".equalsIgnoreCase(type)) {
                result = executeAndTransformToCSV();
            } else {
                throw new IOException("found unsupported return format type request for [" + type + "]");
            }
            return result;
        }

        private String executeRemote() throws IOException {
            org.apache.http.client.HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://46.4.36.142:7474/db/data/cypher");
            HttpClient.addJsonHeaders(httpPost);
            httpPost.setEntity(new StringEntity(wrapQuery(query, params)));
            BasicResponseHandler responseHandler = new BasicResponseHandler();
            return httpClient.execute(httpPost, responseHandler);
        }

        private String executeAndTransformToCSV() throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(executeRemote());
            StringBuilder resultBuilder = new StringBuilder();
            writeArray(jsonNode, resultBuilder, "columns");
            writeArray(jsonNode, resultBuilder, "data");
            return resultBuilder.toString();
        }

        private void writeArray(JsonNode jsonNode, StringBuilder resultBuilder, String arrayName) {
            JsonNode array = jsonNode.get(arrayName);
            if (array.isArray()) {
                writeArray(resultBuilder, array);
            }
        }

        private void writeArray(StringBuilder resultBuilder, JsonNode array) {
            Iterator<JsonNode> iterator = array.iterator();
            while (iterator.hasNext()) {
                appendValue(resultBuilder, iterator);
            }
        }


        private void appendValue(StringBuilder resultBuilder, Iterator<JsonNode> iterator) {
            JsonNode node = iterator.next();
            if (node.isArray()) {
                writeArray(resultBuilder, node);
            } else {
                writeObject(resultBuilder, iterator, node);
            }
        }

        private void writeObject(StringBuilder resultBuilder, Iterator<JsonNode> iterator, JsonNode node) {
            if (node.isTextual()) {
                resultBuilder.append("\"");
            }
            resultBuilder.append(node.getValueAsText());
            if (node.isTextual()) {
                resultBuilder.append("\"");
            }
            if (iterator.hasNext()) {
                resultBuilder.append(",");
            } else {
                resultBuilder.append("\n");
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