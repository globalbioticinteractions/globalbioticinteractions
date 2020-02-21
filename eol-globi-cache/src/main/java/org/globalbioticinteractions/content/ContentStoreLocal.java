package org.globalbioticinteractions.content;

import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.cache.CacheUtil;
import org.globalbioticinteractions.cache.ContentProvenance;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public class ContentStoreLocal implements ContentStore {

    private final File storeDir;
    private final String namespace;
    private final ContentRegistry contentRegistry;
    private final InputStreamFactory inputStreamFactory;

    public ContentStoreLocal(File storeDir, String namespace, InputStreamFactory inputStreamFactory) {
        this.storeDir = storeDir;
        this.namespace = namespace;
        this.inputStreamFactory = inputStreamFactory;
        this.contentRegistry = new ContentRegistryLocal(storeDir, namespace, inputStreamFactory);
    }

    /**
     *
     * @param source content to be stored
     * @return provenance of the provided and registered content
     * @throws IOException
     */

    @Override
    public ContentProvenance store(ContentSource source) throws IOException {
        URI generatedContentURI = URI.create(UUID.randomUUID().toString());
        File cacheDirForNamespace = CacheUtil.findOrMakeCacheDirForNamespace(storeDir.getAbsolutePath(), namespace);
        InputStream is = getInputStreamFactory().create(source.getContent().orElseThrow(() -> new IOException("failed to access content source")));
        ContentProvenance localProvenance = CacheUtil.cacheStream(is, cacheDirForNamespace);
        ContentProvenance provenanceInNamespace = new ContentProvenance(namespace, generatedContentURI, localProvenance.getLocalURI(), localProvenance.getSha256(), localProvenance.getSha256());
        return contentRegistry.register(provenanceInNamespace);
    }

    private InputStreamFactory getInputStreamFactory() {
        return inputStreamFactory;
    }

    private Optional<InputStream> retrieveNow(URI localResourceURI) throws IOException {
        File localFile = new File(localResourceURI);
        return !localFile.exists()
                ? Optional.empty()
                : Optional.of(new FileInputStream(localFile));
    }

    @Override
    public ContentSource retrieve(URI contentHash) {
        return () -> ContentStoreLocal.this.retrieveNow(contentHash);
    }
}
