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
        this(query, params, CypherUtil.CYPHER_VERSION_2_3);
    }

    public CypherQuery(String query, String version) {
        this(query, Collections.emptyMap(), version);
    }

    public CypherQuery(String query, Map<String, String> params, String version) {
        this.query = query;
        this.params = params == null ? Collections.emptyMap() : params;
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
