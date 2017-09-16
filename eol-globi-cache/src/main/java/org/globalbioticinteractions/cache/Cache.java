package org.globalbioticinteractions.cache;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface Cache {
    URI asURI(URI resourceURI) throws IOException;

    CachedURI asMeta(URI resourceURI);

    InputStream asInputStream(URI resourceURI) throws IOException;
}
