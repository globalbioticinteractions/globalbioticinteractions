package org.eol.globi.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface ResourceService {

    InputStream getResource(String resourceName) throws IOException;

    URI getResourceURI(String resourceName);

}
