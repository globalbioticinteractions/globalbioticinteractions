package org.eol.globi.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

interface BlobStore {
    InputStream asInputStream(URI resource, Class clazz) throws IOException;

    InputStream asInputStream(final String resource, Class clazz) throws IOException;

    URI getAbsoluteResourceURI(URI context, String resourceName);

    boolean resourceExists(URI descriptor);
}
