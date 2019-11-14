package org.eol.globi.util;

import java.util.Collections;
import java.util.Map;

public class CypherQuery {
    private final String query;

    private final Map<String, String> params;

    private final String version;

    public CypherQuery(String query) {
        this(query, Collections.emptyMap());
    }

    public CypherQuery(String query, Map<String, String> params) {
        this(query, params, "2.3");
    }

    public CypherQuery(String query, String version) {
        this(query, null, version);
    }

    public CypherQuery(String query, Map<String, String> params, String version) {
        this.query = query;
        this.params = params;
        this.version = version;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getQuery() {
        return query.replaceAll("\\s+", " ");
    }

    public String getVersionedQuery() {
        return ("CYPHER " + version + " " + getQuery());
    }

    public String getVersion() {
        return version;
    }
}
