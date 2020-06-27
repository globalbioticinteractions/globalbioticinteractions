package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetImpl implements Dataset {
    private static final Log LOG = LogFactory.getLog(DatasetImpl.class);

    private final String namespace;
    private final URI archiveURI;
    private final ResourceService resourceService;
    private JsonNode config;
    private URI configURI;

    public DatasetImpl(String namespace, URI archiveURI, final InputStreamFactory factory) {
        this.namespace = namespace;
        this.archiveURI = archiveURI;
        this.resourceService = new ResourceServiceWithMapping(factory, this);
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        return resourceService.retrieve(resourceName);
    }

    @Override
    public URI getArchiveURI() {
        return archiveURI;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void setConfig(JsonNode node) {
        this.config = node;
    }

    @Override
    public JsonNode getConfig() {
        return config;
    }

    @Override
    public String getCitation() {
        return CitationUtil.citationFor(this);
    }

    @Override
    public String getFormat() {
        return getOrDefault("format", "globi");
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return DatasetUtil.getValueOrDefault(getConfig(), key, defaultValue);
    }

    @Override
    public DOI getDOI() {
        String doi = getOrDefault("doi", "");
        try {
            String doiScrubbed = StringUtils.replace(doi, "doi:http://dx.doi.org/", "https://doi.org/");
            return StringUtils.isBlank(doiScrubbed) ? null : DOI.create(doiScrubbed);
        } catch (MalformedDOIException e) {
            LOG.warn("found malformed doi [" + doi + "", e);
            return null;
        }
    }

    public void setConfigURI(URI configURI) {
        this.configURI = configURI;
    }

    @Override
    public URI getConfigURI() {
        return configURI;
    }

}
