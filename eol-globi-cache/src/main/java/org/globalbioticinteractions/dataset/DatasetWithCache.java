package org.globalbioticinteractions.dataset;

import org.codehaus.jackson.JsonNode;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetMapped;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.cache.Cache;
import org.globalbioticinteractions.cache.CachedURI;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Date;

import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.startsWith;
import static org.apache.commons.lang.StringUtils.trim;

public class DatasetWithCache extends DatasetMapped {
    private final Cache cache;
    private final Dataset datasetCached;

    public DatasetWithCache(Dataset dataset, Cache cache) {
        this.datasetCached = dataset;
        this.cache = cache;
    }

    @Override
    public InputStream getResource(String resourceName) throws IOException {
        return cache.asInputStream(getResourceURI2(resourceName));
    }

    @Override
    public URI getResourceURI(String resourceName) {
        URI uri;
        try {
            uri = getResourceURI2(resourceName);
        } catch (IOException e) {
            throw new RuntimeException("failed to get resource [" + resourceName + "]", e);
        }
        return uri;
    }

    private URI getResourceURI2(String resourceName) throws IOException {
        String mappedResourceName = mapResourceNameIfRequested(resourceName, this.getConfig());

        URI resourceURI = URI.create(mappedResourceName);

        URI uri;
        if (resourceURI.isAbsolute()) {
            uri = resourceURI;
        } else {
            URI localArchiveURI = cache.asURI(getArchiveURI());
            URI archiveJarURI = DatasetFinderUtil.getLocalDatasetURIRoot(new File(localArchiveURI));
            uri = ResourceUtil.getAbsoluteResourceURI(archiveJarURI, resourceName);
        }
        return uri;
    }

    private Date getAccessedAt() {
        CachedURI cachedUri = cache.asMeta(getDatasetCached().getArchiveURI());
        return cachedUri == null ? null : cachedUri.getAccessedAt();
    }

    private String getHash() {
        CachedURI cachedUri = cache.asMeta(getDatasetCached().getArchiveURI());
        return cachedUri == null ? null : cachedUri.getSha256();
    }

    public URI getArchiveURI() {
        return getDatasetCached().getArchiveURI();
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        if (equalsIgnoreCase("accessedAt", key)) {
            Date accessedAt = getAccessedAt();
            return accessedAt == null ? "" : ISODateTimeFormat.dateTime().withZoneUTC().print(accessedAt.getTime());
        } else if (equalsIgnoreCase("contentHash", key)) {
            return getHash();
        } else {
            return datasetCached.getOrDefault(key, defaultValue);
        }
    }

    public String getNamespace() {
        return getDatasetCached().getNamespace();
    }

    public JsonNode getConfig() {
        return getDatasetCached().getConfig();
    }

    public String getDOI() {
        String doi = getDatasetCached().getDOI();
        if (isBlank(doi) && startsWith(getArchiveURI().toString(), CitationUtil.ZENODO_URL_PREFIX)) {
            doi = CitationUtil.getDOI(this);
        }
        return doi;
    }

    public String getCitation() {
        StringBuilder citationGenerated = new StringBuilder();
        citationGenerated.append(trim(CitationUtil.citationOrDefaultFor(this, "")));
        String doi = getDOI();
        if (isNotBlank(doi)) {
            citationGenerated.append(CitationUtil.separatorFor(citationGenerated.toString()));
            citationGenerated.append("<");
            citationGenerated.append(doi);
            citationGenerated.append(">");
        }
        citationGenerated.append(CitationUtil.separatorFor(citationGenerated.toString()));
        citationGenerated.append("Accessed on ")
                .append(DateTimeFormat.forPattern("dd MMM YYYY").print(getAccessedAt().getTime()))
                .append(" via <")
                .append(getArchiveURI()).append(">.");
        return citationGenerated.toString();
    }

    public String getFormat() {
        return getDatasetCached().getFormat();
    }

    public URI getConfigURI() {
        return getDatasetCached().getConfigURI();
    }

    @Override
    public void setConfig(JsonNode config) {
        getDatasetCached().setConfig(config);
    }

    @Override
    public void setConfigURI(URI configURI) {
        getDatasetCached().setConfigURI(configURI);
    }

    private Dataset getDatasetCached() {
        return datasetCached;
    }
}
