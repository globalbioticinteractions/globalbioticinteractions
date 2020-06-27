package org.globalbioticinteractions.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class CachePullThrough implements Cache {
    private final static Log LOG = LogFactory.getLog(CachePullThrough.class);
    private final String namespace;
    private final String cachePath;
    private final InputStreamFactory inputStreamFactory;

    public CachePullThrough(String namespace, String cachePath) {
        this(namespace, cachePath, inStream -> inStream);
    }

    public CachePullThrough(String namespace, String cachePath, InputStreamFactory factory) {
        this.namespace = namespace;
        this.cachePath = cachePath;
        this.inputStreamFactory = factory;
    }

    static ContentProvenance cache(URI sourceURI, File cacheDir) throws IOException {
        return CacheUtil.cache(sourceURI, cacheDir, inStream -> inStream);
    }

    private ContentProvenance getContentProvenance(URI resourceName) throws IOException {
        File cacheDirForNamespace = CacheUtil.findOrMakeCacheDirForNamespace(cachePath, namespace);
        ContentProvenance localResourceLocation = CacheUtil.cache(resourceName, cacheDirForNamespace, getInputStreamFactory());

        ContentProvenance contentProvenanceWithNamespace = new ContentProvenance(namespace, resourceName, localResourceLocation.getLocalURI(), localResourceLocation.getSha256(), localResourceLocation.getAccessedAt());
        ProvenanceLog.appendProvenanceLog(new File(cachePath), contentProvenanceWithNamespace);
        return contentProvenanceWithNamespace;
    }

    @Override
    public ContentProvenance provenanceOf(URI resourceURI) {
        return null;
    }

    @Override
    public InputStream retrieve(URI resourceURI) throws IOException {
        ContentProvenance provenance = getContentProvenance(resourceURI);
        URI localURI = provenance.getLocalURI();
        return localURI == null ? null : ResourceUtil.asInputStream(localURI, getInputStreamFactory());
    }

    private InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }
}

