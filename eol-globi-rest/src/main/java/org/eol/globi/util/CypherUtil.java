package org.eol.globi.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

public class CypherUtil {
    public static final String CYPHER_VERSION_1_9 = "1.9";
    public static final String CYPHER_VERSION_2_3 = "2.3";

    private static final Log LOG = LogFactory.getLog(CypherUtil.class);

    public static String executeCypherQuery(CypherQuery query) throws IOException {
        HttpPost httpPost = getCypherRequest(query);
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        return HttpUtil.getHttpClient().execute(httpPost, responseHandler);
    }

    public static HttpPost getCypherRequest(CypherQuery query) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(getCypherURI());
        HttpUtil.addJsonHeaders(httpPost);
        httpPost.setEntity(new StringEntity(wrapQuery(query)));
        return httpPost;
    }

    private static String getCypherURI() {
        String value = System.getProperty("neo4j.cypher.uri");
        return StringUtils.isBlank(value) ? "https://neo4j.globalbioticinteractions.org/db/data/cypher" : StringUtils.trim(value);
    }

    private static String wrapQuery(CypherQuery cypherQuery) {
        String query = "{\"query\":\"";
        query += cypherQuery.getVersionedQuery();
        query += " \", \"params\": {" + buildJSONParamList(cypherQuery.getParams()) + " } }";
        return query;
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
        logQuery(query);
        return executeCypherQuery(query);
    }

    private static void logQuery(CypherQuery query) {
        LOG.info("executing query: [" + query.getVersionedQuery() + "] with params [" + query.getParams() + "]");
    }

    public static HttpResponse execute(CypherQuery cypherQuery) throws IOException {
        logQuery(cypherQuery);
        HttpPost req = getCypherRequest(cypherQuery);
        return HttpUtil.getHttpClient().execute(req);
    }
}
