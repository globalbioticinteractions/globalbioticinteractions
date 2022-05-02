package org.globalbioticinteractions.cache;

import org.eol.globi.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class CachePullThrough implements Cache {
    private final String namespace;
    private final String cachePath;
    private ResourceService resourceServiceRemote;
    private ResourceService resourceServiceLocal;

    public CachePullThrough(String namespace,
                            String cachePath,
                            ResourceService resourceServiceRemote,
                            ResourceService resourceServiceLocal) {
        this.namespace = namespace;
        this.cachePath = cachePath;
        this.resourceServiceRemote = resourceServiceRemote;
        this.resourceServiceLocal = resourceServiceLocal;

    }

    static ContentProvenance cache(URI sourceURI, File cacheDir, ResourceService resourceService) throws IOException {
        return CacheUtil.cache(sourceURI, cacheDir, resourceService);
    }

    private ContentProvenance getContentProvenance(URI resourceName, ResourceService resourceService) throws IOException {
        File cacheDirForNamespace = CacheUtil.findOrMakeCacheDirForNamespace(cachePath, namespace);
        ContentProvenance localResourceLocation = cache(resourceName, cacheDirForNamespace, resourceService);

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
        ContentProvenance provenance = getContentProvenance(resourceURI, resourceServiceRemote);
        URI localURI = provenance.getLocalURI();
        return localURI == null
                ? null
                : resourceServiceLocal.retrieve(localURI);
    }

}

