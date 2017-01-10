package org.eol.globi.service;

import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface Dataset {
    InputStream getResource(String resourceName) throws IOException;

    URI getResourceURI(String resourceName);

    URI getArchiveURI();

    String getNamespace();

    JsonNode getConfig();

    String getCitation();

    String getFormat();

    String getOrDefault(String key, String defaultValue);

    String getDOI();

    URI getConfigURI();

    void setConfig(JsonNode config);

    void setConfigURI(URI configURI);
}
