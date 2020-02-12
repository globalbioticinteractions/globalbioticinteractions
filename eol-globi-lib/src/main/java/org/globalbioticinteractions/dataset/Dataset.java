package org.globalbioticinteractions.dataset;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.doi.DOI;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface Dataset extends ResourceService<URI> {

    URI getArchiveURI();

    String getNamespace();

    JsonNode getConfig();

    String getCitation();

    String getFormat();

    String getOrDefault(String key, String defaultValue);

    DOI getDOI();

    URI getConfigURI();

    void setConfig(JsonNode config);

    void setConfigURI(URI configURI);
}
