package org.globalbioticinteractions.cache;

import org.eol.globi.service.ResourceService;
import org.globalbioticinteractions.dataset.HashCalculator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class CachePullThrough implements Cache {
    private final String namespace;
    private final ResourceService resourceService;
    private final ContentPathFactory contentPathFactory;
    private final String dataDir;
    private final String provDir;
    private final HashCalculator hashCalculator;

    public CachePullThrough(String namespace,
                            ResourceService resourceService,
                            ContentPathFactory contentPathFactory,
                            String dataDir,
                            String provDir,
                            HashCalculator hashCalculator) {
        this.namespace = namespace;
        this.dataDir = dataDir;
        this.provDir = provDir;
        this.resourceService = resourceService;
        this.contentPathFactory = contentPathFactory;
        this.hashCalculator = hashCalculator;
    }

    static ContentProvenance cache(URI sourceURI, File dataDir, ResourceService resourceService, ContentPathFactory contentPathFactory, String namespace1, HashCalculator hashCalculator) throws IOException {
        return CacheUtil.cache(sourceURI, dataDir, resourceService, contentPathFactory, namespace1, hashCalculator);
    }

    private ContentProvenance getContentProvenance(URI resourceName, ResourceService resourceService) throws IOException {
        ContentProvenance localResourceLocation =
                cache(resourceName,
                        new File(dataDir),
                        resourceService,
                        contentPathFactory,
                        namespace,
                        hashCalculator);

        ContentProvenance contentProvenanceWithNamespace
                = new ContentProvenance(
                namespace,
                resourceName,
                localResourceLocation.getLocalURI(),
                localResourceLocation.getContentId(),
                localResourceLocation.getAccessedAt()
        );
        ProvenanceLog.appendProvenanceLog(new File(provDir), contentProvenanceWithNamespace);
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

