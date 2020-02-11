package org.eol.globi.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface ResourceService<T> {

    InputStream retrieve(T resourceName) throws IOException;

    URI getLocalURI(T resourceName) throws IOException;

}
