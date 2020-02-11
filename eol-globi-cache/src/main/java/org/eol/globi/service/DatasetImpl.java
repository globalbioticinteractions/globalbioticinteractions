package org.eol.globi.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetImpl implements Dataset {
    private static final Log LOG = LogFactory.getLog(DatasetImpl.class);

    private final String namespace;
    private final URI archiveURI;
    private final ResourceService<URI> resourceService;
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
    public URI getResourceURI(URI resourceName) throws IOException {
        return resourceService.getResourceURI(resourceName);
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

    private class ResourceServiceWithMapping implements ResourceService<URI> {
        private final InputStreamFactory factory;
        private final Dataset dataset;

        public ResourceServiceWithMapping(InputStreamFactory factory, Dataset dataset) {
            this.factory = factory;
            this.dataset = dataset;
        }

        @Override
        public InputStream retrieve(URI resourceName) throws IOException {
            URI mappedResource = DatasetUtil.getNamedResourceURI(dataset, resourceName);
            return ResourceUtil.asInputStream(getResourceURI(mappedResource).toString(), factory);
        }

        @Override
        public URI getResourceURI(URI resourceName) throws IOException {
            URI mappedResource = DatasetUtil.getNamedResourceURI(dataset, resourceName);
            return ResourceUtil.getAbsoluteResourceURI(getArchiveURI(), mappedResource);
        }
    }
}
