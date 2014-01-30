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
    private final String query;
    private final Map<String, String> params;

    public CypherQueryExecutor(String query, Map<String, String> queryParams) {
        this.query = query;
        this.params = queryParams;
    }

    public CypherQueryExecutor(CypherQuery cypherQuery) {
        this.query = cypherQuery.getQuery();
        this.params = cypherQuery.getParams();

    }

    public String execute(HttpServletRequest request) throws IOException {
        LOG.info("executing query: [" + query + "] with params [" + params + "]");
        String type = request == null ? "json" : request.getParameter("type");
        ResultFormatter formatter = new ResultFormatterFactory().create(type);
        if (formatter == null) {
            throw new IOException("found unsupported return format type request for [" + type + "]");
        } else {
            return formatter.format(executeRemote());
        }
    }

    private String executeRemote() throws IOException {
        return CypherUtil.executeCypherQuery(query, params);
    }


    public static JsonNode parse(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(content);
    }


}
