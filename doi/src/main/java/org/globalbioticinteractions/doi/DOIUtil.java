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

    static URI URIForDoi(DOI doi) {
        return URIForDoi(doi, URI.create(SECURE_DEFAULT_RESOLVER));
    }
    static URI URIForDoi(DOI doi, URI resolverURI) {
        URI uri = null;
        try {
            uri = new URI(resolverURI.getScheme(), resolverURI.getHost(), "/" + doi.toString(), null);
        } catch (URISyntaxException e) {
            // ignore
        }
        return uri;
    }

    static String stripDOIPrefix(String doi) throws MalformedDOIException {
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
                    throw new MalformedDOIException("found unescaped doi in uri [" + doi + "]", e);
                }
            }
        }
        return doi;
    }

}
