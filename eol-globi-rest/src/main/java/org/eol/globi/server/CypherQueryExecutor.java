package org.eol.globi.server;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
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
        long offset = getValueOrDefault(request, "offset", 0L);
        cypherQuery.getParams().put("skip", Long.toString(offset));
        long limit = getValueOrDefault(request, "limit", 1024L);
        cypherQuery.getParams().put("limit", Long.toString(limit));

        String pagedQuery = cypherQuery.getQuery() + " SKIP {skip} LIMIT {limit}";

        LOG.info("executing query: [" + pagedQuery + "] with params [" + cypherQuery.getParams() + "]");
        String type = request == null ? "json" : request.getParameter("type");
        ResultFormatter formatter = new ResultFormatterFactory().create(type);
        if (formatter == null) {
            throw new IOException("found unsupported return format type request for [" + type + "]");
        } else {
            return formatter.format(executeRemote());
        }
    }

    private long getValueOrDefault(HttpServletRequest request, String paramName, long defaultValue) {
        String offsetValue = request.getParameter(paramName);
        long offset = defaultValue;
        if (StringUtils.isNotBlank(offsetValue)) {
            try {
                offset = Long.parseLong(offsetValue);
            } catch(NumberFormatException ex) {
                LOG.warn("malformed " + paramName + " found [" + offsetValue + "]", ex);
            }
        }
        return offset;
    }

    private String executeRemote() throws IOException {
        return CypherUtil.executeCypherQuery(cypherQuery);
    }


    public static JsonNode parse(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(content);
    }


}
