package org.globalbioticinteractions.cache;

import org.eol.globi.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface Cache extends ResourceService<URI> {
    CachedURI asMeta(URI resourceURI);
}
