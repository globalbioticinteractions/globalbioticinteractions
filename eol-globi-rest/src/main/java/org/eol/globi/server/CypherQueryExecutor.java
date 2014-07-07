package org.eol.globi.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.server.util.ResultFormatter;
import org.eol.globi.server.util.ResultFormatterFactory;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

public class CypherQueryExecutor {
    private static final Log LOG = LogFactory.getLog(CypherQueryExecutor.class);
    private final CypherQuery cypherQuery;

    public CypherQueryExecutor(String query, Map<String, String> queryParams) {
        this(new CypherQuery(query, queryParams));
    }

    public CypherQueryExecutor(CypherQuery cypherQuery) {
        this.cypherQuery = cypherQuery;
    }

    public String execute(HttpServletRequest request) throws IOException {
        return execute(request, true);
    }

    public String execute(HttpServletRequest request, boolean enablePaging) throws IOException {
        String type = request == null ? "json" : request.getParameter("type");
        ResultFormatter formatter = new ResultFormatterFactory().create(type);
        if (formatter == null) {
            throw new IOException("found unsupported return format type request for [" + type + "]");
        } else {
            CypherQuery queryToBeExecuted = cypherQuery;
            if (enablePaging) {
                queryToBeExecuted = CypherQueryBuilder.createPagedQuery(request, cypherQuery);
            }
            return formatter.format(executeRemote(queryToBeExecuted));
        }
    }

    private static String executeRemote(CypherQuery query) throws IOException {
        LOG.info("executing query: [" + query.getQuery() + "] with params [" + query.getParams() + "]");
        return CypherUtil.executeCypherQuery(query);
    }


    public static JsonNode parse(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(content);
    }


}
