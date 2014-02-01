package org.eol.globi.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

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
        String type = request == null ? "json" : request.getParameter("type");
        ResultFormatter formatter = new ResultFormatterFactory().create(type);
        if (formatter == null) {
            throw new IOException("found unsupported return format type request for [" + type + "]");
        } else {
            return formatter.format(executeRemote(CypherQueryBuilder.createPagedQuery(request, cypherQuery)));
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
