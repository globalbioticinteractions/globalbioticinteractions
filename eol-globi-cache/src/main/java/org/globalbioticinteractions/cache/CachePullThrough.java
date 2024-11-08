package org.globalbioticinteractions.cache;

import org.eol.globi.service.ResourceService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class CachePullThrough implements Cache {
    private final String namespace;
    private final String cachePath;
    private final ResourceService resourceService;
    private final ContentPathFactory contentPathFactory;

    public CachePullThrough(String namespace,
                            String cachePath,
                            ResourceService resourceService,
                            ContentPathFactory contentPathFactory) {
        this.namespace = namespace;
        this.cachePath = cachePath;
        this.resourceService = resourceService;
        this.contentPathFactory = contentPathFactory;
    }

    static ContentProvenance cache(URI sourceURI, File cacheDir, ResourceService resourceService, ContentPathFactory contentPathFactory, String namespace1) throws IOException {
        return CacheUtil.cache(sourceURI, cacheDir, resourceService, contentPathFactory, namespace1);
    }

    private ContentProvenance getContentProvenance(URI resourceName, ResourceService resourceService) throws IOException {
        ContentProvenance localResourceLocation =
                cache(resourceName,
                        new File(cachePath),
                        resourceService,
                        contentPathFactory,
                        namespace);

        ContentProvenance contentProvenanceWithNamespace
                = new ContentProvenance(
                namespace,
                resourceName,
                localResourceLocation.getLocalURI(),
                localResourceLocation.getSha256(),
                localResourceLocation.getAccessedAt()
        );
        ProvenanceLog.appendProvenanceLog(new File(cachePath), contentProvenanceWithNamespace);
        return contentProvenanceWithNamespace;
    }

    @Override
    public ContentProvenance provenanceOf(URI resourceURI) {
        return null;
    }

    @Override
    public InputStream retrieve(URI resourceURI) throws IOException {
        ContentProvenance provenance = getContentProvenance(resourceURI, resourceService);
        URI localURI = provenance.getLocalURI();
        return localURI == null
                ? null
                : resourceService.retrieve(localURI);
    }

}

