package org.globalbioticinteractions.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.service.Dataset;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.dataset.DatasetFinderUtil;
import org.globalbioticinteractions.dataset.DatasetUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;

public class CacheProxyForDataset extends CacheProxy {
    private static final Log LOG = LogFactory.getLog(CacheProxyForDataset.class);

    private Dataset dataset;

    public CacheProxyForDataset(Cache cache, Dataset dataset) {
        super(Collections.singletonList(cache));
        this.dataset = dataset;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        URI resourceLocation = redirectResourceLocationIfNeeded(resourceName);
        if (null == resourceLocation) {
            throw new IOException("resource [" + resourceName + "] not found");
        }
        InputStream inputStream = super.retrieve(resourceLocation);
        if (null == inputStream) {
            throw new IOException("resource [" + resourceName + "] not found");
        }
        return inputStream;
    }

    @Override
    public URI getLocalURI(URI resourceName) {
        URI uri = null;
        try {
            uri = redirectResourceLocationIfNeeded(resourceName);
        } catch (IOException e) {
            CacheProxyForDataset.LOG.warn("failed to get resource [" + resourceName + "]", e);
        }
        return uri;
    }

    private URI redirectResourceLocationIfNeeded(URI resourceName1) throws IOException {
        URI mappedResourceName = DatasetUtil.getNamedResourceURI(dataset, resourceName1);

        URI uri;
        if (mappedResourceName.isAbsolute()) {
            if (CacheUtil.isLocalDir(mappedResourceName)) {
                uri = mappedResourceName;
            } else {
                uri = super.getLocalURI(mappedResourceName);
            }
        } else {
            URI archiveURI = dataset.getArchiveURI();
            uri = CacheUtil.isLocalDir(archiveURI)
                    ? cacheFileInLocalDirectory(mappedResourceName, archiveURI)
                    : cacheRemoteArchive(mappedResourceName, archiveURI);
        }
        return uri;
    }

    private URI cacheRemoteArchive(URI mappedResourceName, URI archiveURI) throws IOException {
        URI localArchiveURI = super.getLocalURI(archiveURI);
        URI localDatasetRoot = DatasetFinderUtil.getLocalDatasetURIRoot(new File(localArchiveURI));
        return ResourceUtil.getAbsoluteResourceURI(localDatasetRoot, mappedResourceName);
    }

    private URI cacheFileInLocalDirectory(URI mappedResourceName, URI archiveURI) throws IOException {
        URI absoluteResourceURI = ResourceUtil.getAbsoluteResourceURI(archiveURI, mappedResourceName);
        return CacheUtil.isLocalDir(absoluteResourceURI)
                ? absoluteResourceURI
                : super.getLocalURI(absoluteResourceURI);
    }


}
