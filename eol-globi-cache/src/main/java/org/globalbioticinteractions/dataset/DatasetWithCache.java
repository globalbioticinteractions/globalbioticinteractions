package org.globalbioticinteractions.dataset;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.globalbioticinteractions.cache.Cache;
import org.globalbioticinteractions.cache.CacheProxyForDataset;
import org.globalbioticinteractions.cache.ContentProvenance;
import org.globalbioticinteractions.doi.DOI;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.trim;

public class DatasetWithCache implements Dataset {
    private final static Logger LOG = LoggerFactory.getLogger(DatasetWithCache.class);

    private final Cache cache;
    private final Dataset datasetCached;
    private ContentProvenance datasetProvenance;

    public DatasetWithCache(Dataset dataset, final Cache cache) {
        this.datasetCached = dataset;
        this.cache = new CacheProxyForDataset(cache, dataset);

    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        return cache.retrieve(resourceName);
    }


    private String getAccessedAt() {
        return getDatasetProvenance() == null ? null : getDatasetProvenance().getAccessedAt();
    }

    private ContentProvenance getDatasetProvenance() {
        if (this.datasetProvenance == null) {
            this.datasetProvenance = cache.provenanceOf(getDatasetCached().getArchiveURI());
        }
        return this.datasetProvenance;
    }

    private String getHash() {
        return getDatasetProvenance() == null ? null : getDatasetProvenance().getContentId();
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
            return getDatasetCached().getOrDefault(key, defaultValue);
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

    @Override
    public void setExternalId(String externalId) {
        //
    }

    @Override
    public String getExternalId() {
        return getArchiveURI().toString();
    }
}
