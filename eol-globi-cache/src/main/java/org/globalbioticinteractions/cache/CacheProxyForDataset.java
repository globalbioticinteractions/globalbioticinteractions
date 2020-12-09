package org.globalbioticinteractions.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetFinderUtil;
import org.globalbioticinteractions.dataset.DatasetUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;

public class CacheProxyForDataset extends CacheProxy {
    private static final Logger LOG = LoggerFactory.getLogger(CacheProxyForDataset.class);

    private Dataset dataset;

    public CacheProxyForDataset(Cache cache, Dataset dataset) {
        super(Collections.singletonList(cache));
        this.dataset = dataset;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        URI mappedResourceName = DatasetUtil.getNamedResourceURI(dataset, resourceName);

        URI uri;
        if (mappedResourceName.isAbsolute()) {
            uri = mappedResourceName;
        } else {
            URI archiveURI = dataset.getArchiveURI();
            if (CacheUtil.isLocalDir(archiveURI)) {
                uri = ResourceUtil.getAbsoluteResourceURI(archiveURI, mappedResourceName);
            } else {
                // assume remote archive
                InputStream is = super.retrieve(archiveURI);
                if (is == null) {
                    throw new IOException("failed to retrieve [" + archiveURI + "]");
                }
                String localDatasetRoot = DatasetFinderUtil.getLocalDatasetURIRoot(is);
                ContentProvenance contentProvenance = provenanceOf(archiveURI);
                if (contentProvenance == null) {
                    throw new IOException("failed to cache [" + archiveURI + "]");
                }
                URI localArchiveRoot = URI.create("jar:" + contentProvenance.getLocalURI() + "!/" + localDatasetRoot);
                uri = ResourceUtil.getAbsoluteResourceURI(localArchiveRoot, mappedResourceName);
            }
        }
        URI resourceLocation = uri;
        if (null == resourceLocation) {
            throw new IOException("resource [" + resourceName + "] not found");
        }
        InputStream inputStream = super.retrieve(resourceLocation);
        if (null == inputStream) {
            throw new IOException("resource [" + resourceName + "] not found at [" + resourceLocation +"]");
        }
        return inputStream;
    }


}
