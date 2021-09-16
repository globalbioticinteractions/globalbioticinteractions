package org.eol.globi.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CypherUtil {
    public static final String CYPHER_VERSION_2_3 = "2.3";

    private static final Logger LOG = LoggerFactory.getLogger(CypherUtil.class);

    public static String executeCypherQuery(CypherQuery query) throws IOException {
        HttpPost httpPost = getCypherRequest(query);
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        return HttpUtil.getHttpClient().execute(httpPost, responseHandler);
    }

    public static HttpPost getCypherRequest(CypherQuery query) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(getCypherURI());
        HttpUtil.addJsonHeaders(httpPost);
        String queryJson = wrapQuery(query);
        System.out.println(queryJson);
        httpPost.setEntity(new StringEntity(queryJson));
        return httpPost;
    }

    private static String getCypherURI() {
        String value = System.getProperty("neo4j.cypher.uri");
        return StringUtils.isBlank(value)
                ? "https://neo4j.globalbioticinteractions.org/db/data/transaction/commit"
                : StringUtils.trim(value);
    }

    private static String wrapQuery(CypherQuery cypherQuery) {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode req = objectMapper.createObjectNode();

        ArrayNode statements = objectMapper.createArrayNode();
        req.set("statements", statements);

        ObjectNode statementObj = objectMapper.createObjectNode();
        statementObj.put("statement", cypherQuery.getVersionedQuery());
        statements.add(statementObj);

        ObjectNode parameters = objectMapper.createObjectNode();
        statementObj.set("parameters", parameters);
        for (Map.Entry<String, String> entry : cypherQuery.getParams().entrySet()) {
            parameters.put(entry.getKey(), entry.getValue());
        }
        return req.toPrettyString();
    }

    private static String buildJSONParamList(Map<String, String> paramMap) {
        StringBuilder builder = new StringBuilder();
        if (paramMap != null) {
            populateParams(paramMap, builder);
        }
        return builder.toString();
    }

    private static void populateParams(Map<String, String> paramMap, StringBuilder builder) {
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

    public static String executeRemote(CypherQuery query) throws IOException {
        StopWatch stopWatch = logQueryStart(query);
        String result = executeCypherQuery(query);
        logQueryFinish(query, stopWatch);
        return result;
    }

    private static void logQuery(CypherQuery query, String status) {
        LOG.info(status + " query: [" + query.getVersionedQuery() + "] with params [" + query.getParams() + "]");
    }

    private static void logSlowQuery(CypherQuery query, String status) {
        LOG.warn(status + " query: [" + query.getVersionedQuery() + "] with params [" + query.getParams() + "]");
    }

    public static HttpResponse execute(CypherQuery cypherQuery) throws IOException {
        StopWatch stopWatch = logQueryStart(cypherQuery);
        HttpPost req = getCypherRequest(cypherQuery);
        logQueryFinish(cypherQuery, stopWatch);
        return HttpUtil.getHttpClient().execute(req);
    }

    private static void logQueryFinish(CypherQuery cypherQuery, StopWatch stopWatch) {
        stopWatch.stop();
        long delayMs = stopWatch.getTime(TimeUnit.MILLISECONDS);
        logQuery(cypherQuery, "completed (" + delayMs + "ms)");
        if (delayMs > 5 * 60 * 1000) {
            logSlowQuery(cypherQuery, "slow (" + delayMs + "ms)");
        }
    }

    private static StopWatch logQueryStart(CypherQuery cypherQuery) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        logQuery(cypherQuery, "executing");
        return stopWatch;
    }
}
