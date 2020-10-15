package org.globalbioticinteractions.util;

import java.net.URI;
import java.net.URISyntaxException;

public class SparqlUtil {
    public static URI createRequestURI(URI endpoint, String queryString) throws URISyntaxException {
        return new URI(endpoint.getScheme(), endpoint.getHost(), endpoint.getPath(), "query=" + queryString, null);
    }
}
