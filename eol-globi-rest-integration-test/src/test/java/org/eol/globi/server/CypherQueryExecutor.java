package org.eol.globi.server;

import org.eol.globi.server.util.ResultFormatter;
import org.eol.globi.server.util.ResultTestFormatterFactory;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class CypherQueryExecutor {
    private final CypherQuery cypherQuery;

    public CypherQueryExecutor(CypherQuery cypherQuery) {
        this.cypherQuery = cypherQuery;
    }

    protected String execute(HttpServletRequest request) throws IOException {
        String type = request == null ? "json" : request.getParameter("type");
        ResultFormatter formatter = new ResultTestFormatterFactory().create(type);
        if (formatter == null) {
            throw new IOException("found unsupported return format type request for [" + type + "]");
        } else {
            return formatter.format(CypherUtil.executeRemote(cypherQuery));
        }
    }

}
