package org.globalbioticinteractions.content;

import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.cache.CacheLocalReadonly;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.ContentProvenance;
import org.globalbioticinteractions.cache.ProvenanceLog;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.stream.Stream;

public class ContentRegistryLocal implements ContentRegistry, ContentResolver {

    private final String namespace;
    private final File storeDir;

    public ContentRegistryLocal(File storeDir,
                                String namespace,
                                InputStreamFactory inputStreamFactory) {
        this.namespace = namespace;
        this.storeDir = storeDir;
    }


    @Override
    public ContentProvenance register(ContentProvenance contentProvenance) throws IOException {
        ProvenanceLog.appendProvenanceLog(getStoreDir(), contentProvenance);
        return contentProvenance;
    }

    @Override
    public Stream<ContentProvenance> query(URI knownContentIdentifier) {
        ContentProvenance contentProvenance = CacheLocalReadonly
                .getContentProvenance(knownContentIdentifier,
                        storeDir.getAbsolutePath(),
                        namespace);
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
