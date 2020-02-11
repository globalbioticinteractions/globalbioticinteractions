package org.globalbioticinteractions.content;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.cache.Cache;
import org.globalbioticinteractions.cache.CacheLocalReadonly;
import org.globalbioticinteractions.cache.CachePullThrough;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.ContentProvenance;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public class ContentStoreLocal implements ContentStore {

    private final Cache readOnlyLocalCache;
    private final Cache cache;
    private final File storeDir;
    private final String namespace;
    private final ContentRegistry contentRegistry;

    public ContentStoreLocal(File storeDir, String namespace) {
        this.storeDir = storeDir;
        this.namespace = namespace;
        contentRegistry = new ContentRegistryLocal(storeDir, namespace);
        cache = new CachePullThrough(
                this.namespace,
                storeDir.getAbsolutePath(),
                in -> in);
        readOnlyLocalCache = new CacheLocalReadonly(
                namespace,
                storeDir.getAbsolutePath(),
                in -> in);
    }

    /**
     *
     * @param is stream of content to be stored
     * @return provenance of the provided and registered content
     * @throws IOException
     */

    @Override
    public ContentProvenance provideAndRegister(InputStream is) throws IOException {
        URI generatedContentURI = URI.create(UUID.randomUUID().toString());
        File cacheDirForNamespace = CacheUtil.getCacheDirForNamespace(storeDir.getAbsolutePath(), namespace);
        ContentProvenance localProvenance = CachePullThrough.cacheStream(is, cacheDirForNamespace);
        ContentProvenance provenanceInNamespace = new ContentProvenance(namespace, generatedContentURI, localProvenance.getLocalURI(), localProvenance.getSha256(), localProvenance.getSha256());
        return contentRegistry.register(provenanceInNamespace);
    }

    @Override
    public ContentProvenance provideAndRegister(URI contentLocationURI) throws IOException {
        cache.getLocalURI(contentLocationURI);
        ContentProvenance contentProvenance = readOnlyLocalCache.provenanceOf(contentLocationURI);
        if (contentProvenance == null || StringUtils.isBlank(contentProvenance.getSha256())) {
            throw new IOException("failed to store [" + contentLocationURI + "]");
        }
        return contentRegistry.register(contentProvenance);
    }

    @Override
    public Optional<InputStream> retrieve(URI contentHash) throws IOException {
        ContentProvenance contentProvenance = readOnlyLocalCache.provenanceOf(contentHash);
        return contentProvenance == null || contentProvenance.getLocalURI() == null
                ? Optional.empty()
                : Optional.of(ResourceUtil.asInputStream(contentProvenance.getLocalURI().toString()));
    }
}
