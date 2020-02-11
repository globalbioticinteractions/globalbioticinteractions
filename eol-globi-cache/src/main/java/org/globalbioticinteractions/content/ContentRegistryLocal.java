package org.globalbioticinteractions.content;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.cache.Cache;
import org.globalbioticinteractions.cache.CacheLocalReadonly;
import org.globalbioticinteractions.cache.CachePullThrough;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.ContentProvenance;
import org.globalbioticinteractions.cache.ProvenanceLog;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

public class ContentRegistryLocal implements ContentRegistry {

    private final Cache cache;
    private final Cache readOnlyLocalCache;
    private final InputStreamFactory inputStreamFactory;
    private final String namespace;
    private final File storeDir;

    public ContentRegistryLocal(File storeDir, String namespace) {
        this.inputStreamFactory = in -> in;
        this.namespace = namespace;
        this.storeDir = storeDir;
        cache = new CachePullThrough(
                this.namespace,
                storeDir.getAbsolutePath(),
                inputStreamFactory);
        readOnlyLocalCache = new CacheLocalReadonly(
                this.namespace,
                storeDir.getAbsolutePath(),
                inputStreamFactory);
    }


    @Override
    public ContentProvenance register(ContentProvenance contentProvenance) throws IOException {
        File cacheDirForNamespace = CacheUtil.getCacheDirForNamespace(getStoreDir().getAbsolutePath(), getNamespace());
        ProvenanceLog.appendProvenanceLog(cacheDirForNamespace, contentProvenance);
        return contentProvenance;
    }

    @Override
    public Stream<ContentProvenance> resolve(URI knownContentIdentifier) {
        ContentProvenance contentProvenance = readOnlyLocalCache.provenanceOf(knownContentIdentifier);
        return contentProvenance == null
                ? Stream.empty()
                : Stream.of(contentProvenance);
    }

    public String getNamespace() {
        return namespace;
    }

    public File getStoreDir() {
        return storeDir;
    }
}
