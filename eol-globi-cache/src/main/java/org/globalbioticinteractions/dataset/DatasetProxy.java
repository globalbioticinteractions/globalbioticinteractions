package org.globalbioticinteractions.dataset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.globalbioticinteractions.doi.DOI;
import org.globalbioticinteractions.doi.MalformedDOIException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DatasetProxy implements Dataset {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetProxy.class);
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
    public URI getArchiveURI() {
        return datasetProxied.getArchiveURI();
    }

    @Override
    public String getNamespace() {
        return datasetProxied.getNamespace();
    }

    @Override
    public JsonNode getConfig() {
        JsonNode mergedConfig = datasetProxied.getConfig();
        if (mergedConfig != null && config != null) {
            mergedConfig = mergeProxiedConfig(mergedConfig, config);
        }

        return mergedConfig == null ? config : mergedConfig;
    }

    private static JsonNode mergeProxiedConfig(JsonNode config, JsonNode configOverride) {
        ObjectNode merged;
        ObjectMapper mapper = new ObjectMapper();
        try {
            // see https://github.com/FasterXML/jackson-databind/issues/3122
            ObjectNode orig = mapper.readValue(config.toString(), ObjectNode.class);
            JsonNode origSchema = orig.get("tableSchema");
            orig.remove("tableSchema");

            JsonNode origTables = orig.get("tables");
            orig.remove("tables");

            ObjectNode override = mapper.readValue(configOverride.toString(), ObjectNode.class);
            JsonNode overrideSchema = override.get("tableSchema");
            override.remove("tableSchema");

            JsonNode overrideTables = override.get("tables");
            override.remove("tables");


            JsonNode mergedNoSchema = mapper.readerForUpdating(orig)
                    .readValue(override.toString());
            merged = mapper.readValue(mergedNoSchema.toString(), ObjectNode.class);

            override(merged, origSchema, overrideSchema, "tableSchema");
            override(merged, origTables, overrideTables, "tables");

        } catch (JsonProcessingException e) {
            throw new RuntimeException("unexpected json processing error", e);
        }
        return merged;
    }

    private static void override(ObjectNode merged,
                                 JsonNode origTables,
                                 JsonNode overrideTables,
                                 String propertyName) {
        if (overrideTables == null && origTables != null) {
            merged.set(propertyName, origTables);
        } else if (overrideTables != null) {
            merged.set(propertyName, overrideTables);
        }
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
        return (config != null && config.has(key) && config.get(key).isValueNode())
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

    @Override
    public void setExternalId(String externalId) {
        datasetProxied.setExternalId(externalId);
    }

    @Override
    public String getExternalId() {
        return datasetProxied.getExternalId();
    }
}
