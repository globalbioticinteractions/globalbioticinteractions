package org.eol.globi.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface ResourceService {

    InputStream retrieve(URI resourceName) throws IOException;

    @Deprecated
    URI getLocalURI(URI resourceName) throws IOException;

}
