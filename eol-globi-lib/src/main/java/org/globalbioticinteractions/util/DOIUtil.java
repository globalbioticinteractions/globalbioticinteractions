package org.globalbioticinteractions.util;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;

public class DOIUtil {
    public static URI URIfor(String doi) {
        URI uri = null;

        String doiStripped = stripDOIPrefix(doi);

        if (StringUtils.isNotBlank(doiStripped)) {
            try {
                uri = new URI("https", "doi.org", "/" + doiStripped, null);
            } catch (URISyntaxException e) {
                // ignore
            }
        }
        return uri;
    }

    public static String urlForDOI(String doi) {
        URI uri = DOIUtil.URIfor(doi);
        return uri == null ? "" : uri.toString();
    }

    public static String stripDOIPrefix(String doi) {
        return StringUtils.isNotBlank(doi)
                    ? Stream.of("doi:", "https://doi.org/", "http://dx.doi.org/")
                            .reduce(StringUtils.trim(doi), StringUtils::removeStartIgnoreCase)
                    : "";
    }
}
