package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.eol.globi.service.Dataset;
import org.eol.globi.service.DatasetConstant;
import org.eol.globi.service.DatasetMapped;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.cache.Cache;
import org.globalbioticinteractions.cache.CachedURI;
import org.globalbioticinteractions.doi.DOI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.trim;

public class DatasetWithCache extends DatasetMapped {
    private final static Log LOG = LogFactory.getLog(DatasetWithCache.class);

    private final Cache cache;
    private final Dataset datasetCached;
    private CachedURI cachedUri;

    public DatasetWithCache(Dataset dataset, Cache cache) {
        this.datasetCached = dataset;
        this.cache = cache;
    }

    @Override
    public InputStream getResource(String resourceName) throws IOException {
        URI resourceURI2 = getResourceURI2(resourceName);
        if (null == resourceURI2) {
            throw new IOException("resource [" + resourceName + "] not found");
        }
        InputStream inputStream = cache.getResource(resourceURI2);
        if (null == inputStream) {
            throw new IOException("resource [" + resourceName + "] not found");
        }
        return inputStream;
    }

    @Override
    public URI getResourceURI(String resourceName) {
        URI uri = null;
        try {
            uri = getResourceURI2(resourceName);
        } catch (IOException e) {
            LOG.warn("failed to get resource [" + resourceName + "]", e);
        }
        return uri;
    }

    private URI getResourceURI2(String resourceName) throws IOException {
        URI mappedResourceName = mapResourceNameIfRequested(URI.create(resourceName), this.getConfig());

        URI resourceURI = mappedResourceName;

        URI uri;
        if (resourceURI.isAbsolute()) {
            if (isLocalDir(resourceURI)) {
                uri = resourceURI;
            } else {
                uri = cache.getResourceURI(resourceURI);
            }
        } else {
            URI archiveURI = getArchiveURI();
            uri = isLocalDir(archiveURI)
                    ? cacheFileInLocalDirectory(mappedResourceName, archiveURI)
                    : cacheRemoteArchive(mappedResourceName, archiveURI);
        }
        return uri;
    }

    private URI cacheRemoteArchive(URI mappedResourceName, URI archiveURI) throws IOException {
        URI localArchiveURI = cache.getResourceURI(archiveURI);
        URI localDatasetRoot = DatasetFinderUtil.getLocalDatasetURIRoot(new File(localArchiveURI));
        return ResourceUtil.getAbsoluteResourceURI(localDatasetRoot, mappedResourceName);
    }

    private URI cacheFileInLocalDirectory(URI mappedResourceName, URI archiveURI) throws IOException {
        URI absoluteResourceURI = ResourceUtil.getAbsoluteResourceURI(archiveURI, mappedResourceName);
        return isLocalDir(absoluteResourceURI) ? absoluteResourceURI : cache.getResourceURI(absoluteResourceURI);
    }

    public static boolean isLocalDir(URI archiveURI) {
        return archiveURI != null
                && StringUtils.equals("file", archiveURI.getScheme())
                && new File(archiveURI).exists()
                && new File(archiveURI).isDirectory();
    }

    private String getAccessedAt() {
        return getCachedURI() == null ? null : getCachedURI().getAccessedAt();
    }

    private CachedURI getCachedURI() {
        if (this.cachedUri == null) {
            this.cachedUri = cache.asMeta(getDatasetCached().getArchiveURI());
        }
        return this.cachedUri;
    }

    private String getHash() {
        return getCachedURI() == null ? null : getCachedURI().getSha256();
    }

    public URI getArchiveURI() {
        return getDatasetCached().getArchiveURI();
    }

    @Override
    public String getOrDefault(String key, String defaultValue) {
        if (equalsIgnoreCase(DatasetConstant.LAST_SEEN_AT, key)) {
            String accessedAt = getAccessedAt();
            return accessedAt == null ? "" : accessedAt;
        } else if (equalsIgnoreCase(DatasetConstant.CONTENT_HASH, key)) {
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

    public DOI getDOI() {
        DOI doi = getDatasetCached().getDOI();
        if (doi == null && getArchiveURI() != null && startsWith(getArchiveURI().toString(), CitationUtil.ZENODO_URL_PREFIX)) {
            doi = CitationUtil.getDOI(this);
        }
        return doi;
    }

    public String getCitation() {
        String citation = CitationUtil.citationOrDefaultFor(this, "");
        return StringUtils.isBlank(citation) ? generateCitation(citation) : citation;
    }

    private String generateCitation(String citation) {
        StringBuilder citationGenerated = new StringBuilder();
        citationGenerated.append(trim(citation));
        DOI doi = getDOI();
        if (doi != null) {
            citationGenerated.append(CitationUtil.separatorFor(citationGenerated.toString()));
            citationGenerated.append("<");
            citationGenerated.append(doi.toURI());
            citationGenerated.append(">");
        }
        citationGenerated.append(CitationUtil.separatorFor(citationGenerated.toString()));

        citationGenerated.append("Accessed");
        if (null != getAccessedAt()) {
            citationGenerated.append(" on ")
                    .append(getAccessedAt());
        }

        if (null != getArchiveURI()) {
            citationGenerated.append(" via <")
                    .append(getArchiveURI()).append(">.");
        }
        return StringUtils.trim(citationGenerated.toString());
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
