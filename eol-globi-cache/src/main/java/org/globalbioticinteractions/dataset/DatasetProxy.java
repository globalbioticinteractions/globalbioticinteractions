package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.service.Dataset;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetProxy implements Dataset {

    private static final Log LOG = LogFactory.getLog(DatasetProxy.class);
    private JsonNode config;
    private final Dataset datasetProxied;

    public DatasetProxy(Dataset datasetProxied) {
        this.datasetProxied = datasetProxied;
    }

    @Override
    public InputStream retrieve(URI resourcePath) throws IOException {
        return datasetProxied.retrieve(DatasetUtil.getNamedResourceURI(this, resourcePath));
    }

    @Override
    public URI getLocalURI(URI resourcePath) throws IOException {
        return datasetProxied.getLocalURI(DatasetUtil.getNamedResourceURI(this, resourcePath));
    }

    @Override
    public URI getArchiveURI() {
        return datasetProxied.getArchiveURI();
    }

    @Override
    public String getNamespace() {
        return datasetProxied.getNamespace();
    }

    @Override
    public JsonNode getConfig() {
        return (config == null) ? datasetProxied.getConfig() : config;
    }

    @Override
    public String getCitation() {
        return CitationUtil.citationOrDefaultFor(this, datasetProxied.getCitation());
    }

    @Override
    public String getFormat() {
        return DatasetUtil.getValueOrDefault(config, "format", datasetProxied.getFormat());
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        return (config != null && config.has(key))
                ? config.get(key).asText()
                : datasetProxied.getOrDefault(key, defaultValue);
    }

    @Override
    public DOI getDOI() {
        String proxiedDoi = datasetProxied.getDOI() == null ? null : datasetProxied.getDOI().toString();
        String doi = DatasetUtil.getValueOrDefault(config, "doi", proxiedDoi);
        try {
            return StringUtils.isBlank(doi) ? null : DOI.create(doi);
        } catch (MalformedDOIException e) {
            LOG.warn("found malformed doi [" + doi + "]", e);
            return null;
        }
    }

    @Override
    public URI getConfigURI() {
        return datasetProxied.getConfigURI();
    }

    @Override
    public void setConfig(JsonNode config) {
        this.config = config;
    }

    @Override
    public void setConfigURI(URI configURI) {
        datasetProxied.setConfigURI(configURI);
    }
}
