package org.globalbioticinteractions.cache;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetFinderUtil;
import org.globalbioticinteractions.dataset.DatasetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CacheProxyForDataset extends CacheProxy {
    private static final Logger LOG = LoggerFactory.getLogger(CacheProxyForDataset.class);
    public static final Pattern PATH_MATCH = Pattern.compile("[/]{0,1}(?<path>.*)");

    private Dataset dataset;

    public CacheProxyForDataset(Cache cache, Dataset dataset) {
        super(Collections.singletonList(cache));
        this.dataset = dataset;
    }

    @Override
    public InputStream retrieve(URI resourceName) throws IOException {
        URI resourceLocation = getResourceLocation(resourceName);
        if (null == resourceLocation) {
            throw new IOException("resource [" + resourceName + "] not found");
        }
        InputStream inputStream = super.retrieve(resourceLocation);
        if (null == inputStream) {
            throw new IOException("resource [" + resourceName + "] not found at [" + resourceLocation + "]");
        }
        return inputStream;
    }

    private URI getResourceLocation(URI resourceName) throws IOException {
        URI mappedResourceName = DatasetUtil.getNamedResourceURI(dataset, resourceName);

        URI uri;
        if (mappedResourceName.isAbsolute()) {
            uri = mappedResourceName;
        } else {
            URI archiveURI = dataset.getArchiveURI();
            if (CacheUtil.isLocalDir(archiveURI)) {
                uri = ResourceUtil.getAbsoluteResourceURI(archiveURI, mappedResourceName);
            } else if (StringUtils.endsWith(archiveURI.toString(), "/") && StringUtils.startsWith(archiveURI.toString(), "file:/")) {
                Matcher matcher = PATH_MATCH.matcher(resourceName.toString());
                if (!matcher.matches()) {
                    throw new IOException("unexpected mismatch for [" + resourceName + "]");
                }
                URI resourceInDirectoryURI = URI.create(archiveURI.toString() + matcher.group("path"));
                ContentProvenance contentProvenance = provenanceOf(resourceInDirectoryURI);
                if (contentProvenance == null) {
                    throw new IOException("unknown resource [" + resourceInDirectoryURI + "]");
                }
                uri = contentProvenance.getSourceURI();
            } else {
                // resource is embedded in some archive (e.g., zip file)
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
        return uri;
    }


}
