package org.eol.globi.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface ResourceCache {
    InputStream asInputStream(final String resource) throws IOException;

    URI getAbsoluteResourceURI(URI context, String resourceName);

    boolean resourceExists(URI descriptor);
}
