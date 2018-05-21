package org.globalbioticinteractions.util;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class DOIUtil {

    public static final List<String> DOI_URLS = Arrays.asList("https://doi.org/", "http://dx.doi.org/");
    public static final List<String> DOI_SCHEME = Arrays.asList("doi:");

    public static URI URIfor(String doi) {
        URI uri = null;

        String doiStripped = stripDOIPrefix(doi);

        if (isValid(doiStripped)) {
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
        return uri == null ? null : uri.toString();
    }

    static String stripDOIPrefix(String doi) {
        return StringUtils.trim(StringUtils.isBlank(doi)
                ? doi
                : Stream.concat(DOI_SCHEME.stream(), DOI_URLS.stream())
                .reduce(StringUtils.trim(doi), (agg, d) -> {
                    if (StringUtils.startsWithIgnoreCase(agg, d)) {
                        String doiStripped = StringUtils.removeStartIgnoreCase(agg, d);
                        if (DOI_URLS.contains(d)) {
                            URI uri = URI.create("some://host/path?" + doiStripped);
                            agg = uri.getQuery();
                        } else {
                            agg = doiStripped;
                        }
                    }
                    return agg;
                })
        );
    }

    public static boolean isValid(String doi) {
        String strippedDOI = stripDOIPrefix(doi);
        return StringUtils.startsWith(strippedDOI, "10.");
    }

    public static boolean isDoiPrefix(String idPrefix) {
        String prefixLower = StringUtils.lowerCase(idPrefix);
        return DOI_SCHEME.contains(prefixLower) || DOI_URLS.contains(prefixLower);
    }
}
