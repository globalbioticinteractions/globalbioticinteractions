package org.globalbioticinteractions.dataset;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.domain.Citable;
import org.eol.globi.service.ResourceService;

import java.net.URI;

public interface Dataset extends Citable, ResourceService {

    URI getArchiveURI();

    String getNamespace();

    JsonNode getConfig();

    String getFormat();

    String getOrDefault(String key, String defaultValue);

    URI getConfigURI();

    void setConfig(JsonNode config);

    void setConfigURI(URI configURI);

}
