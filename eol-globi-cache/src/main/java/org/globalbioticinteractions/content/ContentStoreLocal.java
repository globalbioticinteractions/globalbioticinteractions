package org.globalbioticinteractions.content;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.ResourceUtil;
import org.globalbioticinteractions.cache.Cache;
import org.globalbioticinteractions.cache.CacheLocalReadonly;
import org.globalbioticinteractions.cache.CacheLog;
import org.globalbioticinteractions.cache.CachePullThrough;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.ContentProvenance;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

public class ContentStoreLocal implements ContentStore {

    private final Cache cache;
    private final Cache readOnlyLocalCache;
    private final File storeDir;
    private final String namespace;

    public ContentStoreLocal(File storeDir, String namespace) {
        this.storeDir = storeDir;
        this.namespace = namespace;
        cache = new CachePullThrough(
                this.namespace,
                storeDir.getAbsolutePath(),
                in -> in);
        readOnlyLocalCache = new CacheLocalReadonly(
                namespace,
                storeDir.getAbsolutePath(),
                in -> in);
    }


    @Override
    public URI save(InputStream is) throws IOException {
        File cacheDirForNamespace = CacheUtil.getCacheDirForNamespace(storeDir.getAbsolutePath(), namespace);
        File file = CachePullThrough.cacheStream(is, cacheDirForNamespace);
        URI contentHash = null;
        if (file != null) {
            CacheLog.appendCacheLog(namespace, URI.create("/dev/stdin"), cacheDirForNamespace, file.toURI());
            contentHash = URI.create(StringUtils.replace(file.getAbsolutePath(), cacheDirForNamespace.getAbsolutePath(), "hash://sha256"));
        }

        return contentHash;
    }

    @Override
    public Optional<InputStream> retrieve(URI contentHash) throws IOException {

        ContentProvenance contentProvenance = readOnlyLocalCache.provenanceOf(contentHash);
        return contentProvenance == null || contentProvenance.getLocalURI() == null
                ? Optional.empty()
                : Optional.of(ResourceUtil.asInputStream(contentProvenance.getLocalURI().toString()));
    }
}
