package org.eol.globi.util;

import java.util.Map;

public class CypherQuery {
    private String query;

    private final Map<String, String> params;

    public CypherQuery(String query) {
        this(query, null);
    }

    public CypherQuery(String query, Map<String, String> params) {
        this.query = query;
        this.params = params;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getQuery() {
        return query.replaceAll("\\s+", " ");
    }

}
