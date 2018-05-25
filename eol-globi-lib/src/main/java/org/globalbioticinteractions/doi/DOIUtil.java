package org.globalbioticinteractions.doi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class DOIUtil {
    private static final String UNSECURE_DEFAULT_RESOLVER = "http://dx.doi.org/";
    private static final String SECURE_DEFAULT_RESOLVER = "https://doi.org/";
    static final List<String> DOI_URLS = Arrays.asList(SECURE_DEFAULT_RESOLVER, UNSECURE_DEFAULT_RESOLVER);
    static final List<String> PRINTABLE_DOI_PREFIX = Collections.singletonList("doi:");

    public static URI URIfor(String doi) {
        return URIfor(doi, URI.create(SECURE_DEFAULT_RESOLVER));
    }

    static URI URIfor(String doi, URI resolverURI) {
        URI uri = null;
        String doiStripped = stripDOIPrefix(doi);

        try {
            uri = new URI(resolverURI.getScheme(), resolverURI.getHost(), "/" + doiStripped, null);
        } catch (URISyntaxException e) {
            // ignore
        }
        return uri;
    }

    public static String urlForDOI(String doi) {
        URI uri = DOIUtil.URIfor(doi);
        return uri == null ? null : uri.toString();
    }

    private static String stripDOIPrefix(String doi) {
        for (String prefix : PRINTABLE_DOI_PREFIX) {
            if (doi.toLowerCase().startsWith(prefix)) {
                return doi.length() > prefix.length() ? doi.substring(prefix.length()) : doi;
            }
        }

        for (String prefix : DOI_URLS) {
            if (doi.length() > prefix.length() && doi.toLowerCase().startsWith(prefix)) {
                try {
                    String doiStripped = doi.substring(prefix.length());
                    URI uri = URI.create("some://host/path?" + doiStripped);
                    return uri.getQuery();
                } catch (IllegalArgumentException e) {
                    // some invalid characters in stripped doi - probably due to invalid url escaping
                    // from historic doi url generator.
                }
            }
        }
        return doi;
    }

}
