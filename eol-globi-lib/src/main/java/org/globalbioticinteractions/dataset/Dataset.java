package org.globalbioticinteractions.dataset;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.doi.DOI;

import java.net.URI;
import java.util.Collection;
import java.util.List;

public interface Dataset extends ResourceService {

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
